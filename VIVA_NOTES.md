# SmartAttend Viva Notes

## Why continuous presence?

A QR code or one-time check-in proves only one moment. SmartAttend models intervals: Enter opens an interval, Exit closes it, re-entry adds another interval, and the final percentage uses confirmed time divided by scheduled time.

## Attendance policy

The demo policy is >=75% Present, 50% to below 75% Review, and below 50% Absent. Late is a separate boolean based on the first accepted entry and a configurable grace period. These are policy examples, not universal academic rules.

## Why three ESP32 nodes later?

Multiple observations can reduce dependence on one noisy RSSI reading. A starting experiment is stable detection from at least two of three classroom beacons for a dwell period, plus a signal-loss timeout. It must be calibrated in the real room; the numbers cannot be copied blindly.

## BLE/RSSI limitations

RSSI is received signal strength, not distance. It changes with phone models, orientation, people, furniture, interference, doors, and walls. It can penetrate walls and can be replayed or spoofed. BLE alone cannot prove who carries the phone, exact location, or fraud-proof attendance.

## Architecture answer

`PresenceSource` separates how presence is observed from how attendance is calculated. The current `SimulatedPresenceSource` can later be replaced by `BlePresenceSource` without rewriting interval tracking, policy boundaries, or UI state. Pure domain code is unit-tested with an injectable clock.

## Firebase answer

Firebase Authentication identifies users. Firestore stores profiles, enrollment, timetable, sessions, evidence, finalized attendance, and audit logs. Security rules—not hidden buttons—must enforce ownership and teacher rights. Student-reported presence remains untrusted evidence; an authorized teacher finalizes records.

## Offline behavior

The app starts without network or Firebase. Sample data, login state, active session timing, accumulated duration, and open entry state are local. Cloud synchronization and conflict handling are a later configured phase.

## Security answer

There is no client-side role selection. Students cannot write final attendance. Teacher corrections require a reason and audit record. Client apps never contain service-account private keys. A production design would also add backend validation, App Check/rate limits, monitoring, and an institutional correction policy.

## Honest prototype limitations

No real BLE scan, no ESP32 validation, no physical-device test yet, no configured Firebase project, no Android/dashboard synchronization, and no claim of exact positioning, identity proof, or anti-proxy security.
