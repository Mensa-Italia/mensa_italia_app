import 'package:timezone/timezone.dart' as tz;

class RangeDateTimeZone {
  tz.TZDateTime? start;
  tz.TZDateTime? end;

  RangeDateTimeZone({this.start, this.end});

  RangeDateTimeZone.fromDateTimeAndZone(
      {required DateTime start,
      required DateTime end,
      required tz.Location location})
      : start = tz.TZDateTime(
            location,
            start.year,
            start.month,
            start.day,
            start.hour,
            start.minute,
            start.second,
            start.millisecond,
            start.microsecond),
        end = tz.TZDateTime(location, end.year, end.month, end.day, end.hour,
            end.minute, end.second, end.millisecond, end.microsecond);

  RangeDateTimeZone.fromDateTime(
      {required DateTime start, required DateTime end})
      : start = tz.TZDateTime(
            tz.local,
            start.year,
            start.month,
            start.day,
            start.hour,
            start.minute,
            start.second,
            start.millisecond,
            start.microsecond),
        end = tz.TZDateTime(tz.local, end.year, end.month, end.day, end.hour,
            end.minute, end.second, end.millisecond, end.microsecond);

  setStart(DateTime start) {
    this.start = tz.TZDateTime(
        tz.local,
        start.year,
        start.month,
        start.day,
        start.hour,
        start.minute,
        start.second,
        start.millisecond,
        start.microsecond);
  }

  setEnd(DateTime end) {
    this.end = tz.TZDateTime(tz.local, end.year, end.month, end.day, end.hour,
        end.minute, end.second, end.millisecond, end.microsecond);
  }

  clearStart() {
    start = null;
  }

  clearEnd() {
    end = null;
  }

  DateTime getStart() {
    return DateTime(start!.year, start!.month, start!.day, start!.hour,
        start!.minute, start!.second, start!.millisecond, start!.microsecond);
  }

  DateTime getEnd() {
    return DateTime(end!.year, end!.month, end!.day, end!.hour, end!.minute,
        end!.second, end!.millisecond, end!.microsecond);
  }

  bool isValid() {
    return start != null && end != null && start!.isBefore(end!);
  }

  bool isValidStart() {
    return start != null;
  }

  bool isValidEnd() {
    return end != null;
  }

  bool isValidRange() {
    return start != null && end != null && start!.isBefore(end!);
  }
}
