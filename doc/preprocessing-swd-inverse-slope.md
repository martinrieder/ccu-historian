# Vorverarbeitung: Schwingende Tür mit inversen Steigungen (Sw.Do.K. inv.)

Der CCU-Historian bietet als zusätzliche Vorverarbeitungsmethode die Variante
der schwingenden Tür (Swinging Door Compression) mit inversen Steigungen. Sie
ist im Datenpunkt-Konfigurationsdialog als *Sw.Do.K. inv.* (Typ 7) auswählbar.

## Funktionsweise

Wie die Standard-Methode *Sw.Do.K.* komprimiert die Variante eine Folge von
Messwerten, sodass die lineare Interpolation zwischen den gespeicherten
Werten um höchstens die Abweichung `E` von den verworfenen Messwerten
abweicht. Der Parameter ist identisch zur Standard-Methode die Bandbreite;
intern gilt `E = Parameter / 2`.

Der Unterschied liegt in der mathematischen Formulierung. Die Standard-
Implementierung berechnet die Steigungen der beiden Türlinien als
Quotienten `dv/dt`. Die Variante arbeitet stattdessen mit den Kehrwerten
(inversen Steigungen):

* obere Türlinie: `dt / (dv - E)`
* untere Türlinie: `dt / (dv + E)`

Es gibt also keine Division durch den Zeitinkrement. Das ist numerisch
günstig bei hochfrequenten Datenströmen mit kleinen oder konstanten
Zeitabständen zwischen den Messwerten. Eine unendliche inverse Steigung
entspricht einer horizontalen Türlinie; die Öffnungs- und Überlappungs-
regeln sind vorzeichenbewusst formuliert und behandeln auch diesen Fall
ohne Sonderbehandlung.

Die Tür öffnet nach oben bzw. unten, wenn die neue inverse Steigung zu
einer weiteren Türlinie gehört. Überlappen sich die beiden Türlinien, wird
der zuletzt gespeicherte Messwert ausgegeben und die Tür am letzten Wert
neu aufgestellt. Ändert sich die Qualität eines Messwerts, wird die
Komprimierung ebenfalls unterbrochen und neu gestartet.

## Eigenschaften

* Die Ausgabe ist äquivalent zur Standard-Methode *Sw.Do.K.*: Die
  Implementierung reproduziert alle Testfälle des Standard-Tests und
  wurde zusätzlich mit randomisierten und adversarialen Datenströmen
  (Zeitinkremente von 1 ms, exakte Treffer der Türlinien mit unendlichen
  inversen Steigungen, Vorzeichenwechsel der Steigungen) gegen die
  Standard-Implementierung verifiziert.
* Der maximale Rekonstruktionsfehler der linearen Interpolation beträgt
  wie bei der Standard-Methode etwa `2 * E` (Torgeometrie), nicht `E`.
* Das Vorverarbeitungs-Flag (linear interpolierbar) bleibt gesetzt, die
  komprimierten Werte können also wie gewohnt interpoliert werden.
* Der Typ wird pro Datenpunkt in der Spalte PREPROC_TYPE gespeichert.
  Der neue Typ wurde als Index 7 ans Ende der Enumeration angehängt;
  bestehende Konfigurationen sind davon nicht betroffen.

## Wann welche Variante verwenden

Für normale Anwendungen sind beide Varianten gleichwertig. Die Variante
mit inversen Steigungen bietet eine erhöhte numerische Robustheit, wenn
Messwerte mit festen oder sehr kleinen Zeitabständen verarbeitet werden.
Da die Ausgaben identisch sind, kann jederzeit zwischen den Varianten
gewechselt werden.

## Implementierung

* `ccu-historian/src/mdz/ccuhistorian/eventprocessing/SwingingDoorInverseSlopeProcessor.groovy`
  – die eigentliche Kompression.
* `ccu-historian/src/mdz/ccuhistorian/eventprocessing/Preprocessor.groovy`
  – Aufnahme in die Enumeration `SWD_INV_COMPR` und die Verarbeitung.
* `ccu-historian/src-webapp/historian/dpconfig.gy`
  – Eintrag *Sw.Do.K. inv.* im Datenpunkt-Konfigurationsdialog.
* `ccu-historian-test/src/mdz/ccuhistorian/eventprocessing/SwingingDoorInverseSlopeTest.groovy`
  – Unittest, Spiegel des Standard-Tests mit Typ 7.
