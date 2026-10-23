/*
    CCU-Historian, a long term archive for the HomeMatic CCU
    Copyright (C) 2011-2017 MDZ (info@ccu-historian.de)
    Copyright (C) 2026 Martin Rieder (inverse slope variant)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package mdz.ccuhistorian.eventprocessing

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import mdz.eventprocessing.BasicProducer
import mdz.eventprocessing.Processor
import mdz.hc.Event
import mdz.hc.ProcessValue

/**
 * Swinging door compression, variant with inverse slopes. Instead of the door
 * slopes (delta value / delta time) their reciprocals (delta time / delta value)
 * are tracked. This avoids divisions by the time increment, which is beneficial
 * for high frequency data streams, and simplifies the comparisons for fixed or
 * small time increments. The compressed output is equivalent to
 * SwingingDoorProcessor.
 */
@CompileStatic
@Log
class SwingingDoorInverseSlopeProcessor extends BasicProducer<Event> implements Processor<Event, Event> {
	
	double deviation
	
	private static class InverseSlopes {
		double upper, lower
	}
	
	private Event first, last
	private InverseSlopes inverseSlopes
	
	public void produce(Event e) {
		// modify state
		e.pv.state |= ProcessValue.STATE_PREPROCESSED
		super.produce e
	}
	
	public void consume(Event e) throws Exception {
		if (first==null) {
			// store first
			produce e
			first=e
			return
		}
		if (e.pv.timestamp<=first.pv.timestamp) {
			log.warning "Swinging door compression: Discarding event with earlier timestamp: $e"
			return
		}
		// quality changed? ignore preprocessed flag.
		if ( (e.pv.state & ~ProcessValue.STATE_PREPROCESSED) != 
			(first.pv.state & ~ProcessValue.STATE_PREPROCESSED) ) {
			// store last, if present
			if (last!=null) {
				produce last
			}
			// store first
			produce e
			// restart
			first=e
			last=null
			inverseSlopes=null
			return
		}
		// check slopes?
		if (inverseSlopes!=null) {
			InverseSlopes slopesTmp=calculateInverseSlopes(first, e)
			// open swinging door
			if (opensUpperDoor(slopesTmp.upper, inverseSlopes.upper)) {
				inverseSlopes.upper=slopesTmp.upper
			}
			if (opensLowerDoor(slopesTmp.lower, inverseSlopes.lower)) {
				inverseSlopes.lower=slopesTmp.lower
			}
			// overwind?
			if (isOverwound(inverseSlopes.upper, inverseSlopes.lower)) {
				// store last
				produce last
				// restart
				first=last
				last=e
				inverseSlopes=calculateInverseSlopes(first, e)
				return
			}
			// value is within slopes
			last=e
			return
		}
		// second event, calculate initial slopes
		last=e
		inverseSlopes=calculateInverseSlopes(first, e)
	}

	public void close() {
		// store last, if present
		if (last!=null) {
			produce last
		}
		first=null
		last=null
		inverseSlopes=null
	}

	public void flush() {
		// store last, if present
		if (last!=null) {
			produce last
			// restart
			first=last
		}
		last=null
		inverseSlopes=null
	}
		
	/**
	 * Calculates the inverse slopes of the two door lines, which start at the
	 * first point +/- the deviation: dt/(dv-deviation) and dt/(dv+deviation).
	 * An infinite inverse slope corresponds to a horizontal door line.
	 */
	private InverseSlopes calculateInverseSlopes(Event begin, Event end) {
		double dt=(double)(end.pv.timestamp.time-begin.pv.timestamp.time)
		double dv=((Number)end.pv.value).doubleValue()-((Number)begin.pv.value).doubleValue()
		new InverseSlopes(upper: dt/(dv-deviation), lower: dt/(dv+deviation))
	}
	
	/**
	 * The upper door opens, if the new inverse upper slope belongs to a greater
	 * upper door slope than the current one. For equal signs of both inverse
	 * slopes this is equivalent to the new inverse slope being smaller. For
	 * different signs it is only the case for a positive new inverse slope.
	 */
	private static boolean opensUpperDoor(double newInv, double curInv) {
		if (newInv==curInv)
			return false
		boolean sameSign=(newInv>0 && curInv>0) || (newInv<0 && curInv<0)
		sameSign ? newInv<curInv : newInv>0
	}
	
	/**
	 * The lower door opens, if the new inverse lower slope belongs to a smaller
	 * lower door slope than the current one. For equal signs of both inverse
	 * slopes this is equivalent to the new inverse slope being greater. For
	 * different signs it is only the case for a negative new inverse slope.
	 */
	private static boolean opensLowerDoor(double newInv, double curInv) {
		if (newInv==curInv)
			return false
		boolean sameSign=(newInv>0 && curInv>0) || (newInv<0 && curInv<0)
		sameSign ? newInv>curInv : newInv<0
	}
	
	/**
	 * The swinging door is overwound, if the upper door slope is greater than
	 * the lower door slope. In inverse slope terms this means for equal signs,
	 * that the upper inverse slope is smaller than the lower inverse slope, and
	 * for different signs, that it is greater.
	 */
	private static boolean isOverwound(double upperInv, double lowerInv) {
		double product=upperInv*lowerInv
		product>0 ? upperInv<lowerInv : upperInv>lowerInv
	}
}
