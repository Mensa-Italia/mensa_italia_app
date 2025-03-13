import 'package:flutter_test/flutter_test.dart';
import 'package:mensa_italia_app/app/app.locator.dart';

import '../helpers/test_helpers.dart';

void main() {
  group('BottomCheckIdentityModel Tests -', () {
    setUp(() => registerServices());
    tearDown(() => locator.reset());
  });
}

