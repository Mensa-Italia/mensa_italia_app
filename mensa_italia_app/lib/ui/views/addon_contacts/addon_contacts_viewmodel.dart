import 'package:faker/faker.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

class AddonContactsViewModel extends BaseViewModel {
  List<String> contacts = [];
  ScrollController scrollController = ScrollController();
  TextEditingController searchController = TextEditingController();

  AddonContactsViewModel() {
    contacts = List.generate(300, (index) => Faker().person.name());
    contacts.sort();
    rebuildUi();
  }

  void search(String value) {}
}
