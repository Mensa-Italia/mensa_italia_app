import 'package:faker/faker.dart';
import 'package:stacked/stacked.dart';

class AddonContactsViewModel extends BaseViewModel {
  List<String> contacts = [];

  AddonContactsViewModel() {
    contacts = List.generate(300, (index) => Faker().person.name());
    contacts.sort();
    rebuildUi();
    
  }
}
