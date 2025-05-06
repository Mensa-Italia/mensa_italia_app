import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:form_validator/form_validator.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';
import 'package:mensa_italia_app/ui/common/ui_helpers.dart';

import 'login_viewmodel.dart';

class LoginView extends StackedView<LoginViewModel> {
  const LoginView({super.key});

  @override
  Widget builder(
      BuildContext context, LoginViewModel viewModel, Widget? child) {
    return Scaffold(
      body: GestureDetector(
        onTap: () => FocusScope.of(context).unfocus(),
        child: Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                Color.fromARGB(255, 252, 226, 248),
                Color.fromARGB(255, 191, 212, 252),
              ],
            ),
          ),
          child: Center(
            child: SingleChildScrollView(
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: MediaQuery.of(context).size.height,
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Hero(
                      tag: "home_logo",
                      child: SvgPicture.asset(
                        "assets/svg/icon_compact.svg",
                        width: 100,
                        height: 100,
                      ),
                    ),
                    Container(
                      margin: const EdgeInsets.symmetric(
                          horizontal: 25, vertical: 60),
                      child: Text(
                        "views.signin.title".tr(),
                        style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    Container(
                      margin: const EdgeInsets.symmetric(horizontal: 30),
                      child: Form(
                        key: viewModel.formKey,
                        autovalidateMode: AutovalidateMode.onUserInteraction,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            TextFormField(
                              decoration: InputDecoration(
                                hintText:
                                    "views.signin.form.field.hint.email".tr(),
                              ),
                              autocorrect: false,
                              enableSuggestions: true,
                              keyboardType: TextInputType.emailAddress,
                              textInputAction: TextInputAction.next,
                              onSaved: viewModel.saveEmail,
                              validator: ValidationBuilder()
                                  .email(
                                    "views.signin.form.field.error.email".tr(),
                                  )
                                  .build(),
                            ),
                            verticalSpaceSmall,
                            TextFormField(
                              decoration: InputDecoration(
                                hintText:
                                    "views.signin.form.field.hint.password"
                                        .tr(),
                              ),
                              textInputAction: TextInputAction.done,
                              autocorrect: false,
                              enableSuggestions: false,
                              obscureText: true,
                              validator: ValidationBuilder()
                                  .minLength(
                                      3,
                                      "views.signin.form.field.error.password"
                                          .tr())
                                  .build(),
                              onSaved: viewModel.savePassword,
                            ),
                            Row(
                              children: [
                                const Expanded(child: SizedBox()),
                                TextButton(
                                  onPressed: viewModel.goToResetPassword,
                                  child: Text.rich(
                                    TextSpan(
                                      text:
                                          "views.signin.form.button.recover_password.text"
                                              .tr(),
                                      children: [
                                        TextSpan(
                                          text:
                                              "views.signin.form.button.recover_password.button"
                                                  .tr(),
                                          style: TextStyle(
                                            color: kcPrimaryColor,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                      ],
                                      style: TextStyle(
                                        fontSize: 14,
                                        color: kcDarkGreyColor,
                                      ),
                                    ),
                                    textAlign: TextAlign.end,
                                  ),
                                ),
                              ],
                            ),
                            verticalSpaceMedium,
                            ElevatedButton(
                              onPressed: viewModel.doLogin,
                              child: viewModel.isBusy
                                  ? LoadingAnimationWidget.beat(
                                      color: Colors.white.withOpacity(.8),
                                      size: 20,
                                    )
                                  : Text(
                                      "views.signin.form.button.submit".tr(),
                                    ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    Container(
                      margin: const EdgeInsets.symmetric(
                              horizontal: 50, vertical: 20)
                          .copyWith(top: 70),
                      child: Text(
                        "views.signin.nosignupinfo".tr(),
                        textAlign: TextAlign.center,
                      ),
                    )
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  LoginViewModel viewModelBuilder(BuildContext context) => LoginViewModel();
}
