import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_svg/svg.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';
import 'package:mensa_italia_app/ui/common/ui_helpers.dart';

import 'login_viewmodel.dart';

class LoginView extends StackedView<LoginViewModel> {
  const LoginView({Key? key}) : super(key: key);

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
                      child: const Text(
                        "Welcome to Mensa Italia,\nSign in to continue",
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
                              decoration: const InputDecoration(
                                hintText: "Email",
                              ),
                              autocorrect: false,
                              enableSuggestions: true,
                              keyboardType: TextInputType.emailAddress,
                              textInputAction: TextInputAction.next,
                              onSaved: viewModel.saveEmail,
                              validator: viewModel.validateEmail,
                            ),
                            verticalSpaceSmall,
                            TextFormField(
                              decoration: const InputDecoration(
                                hintText: "Password",
                              ),
                              textInputAction: TextInputAction.done,
                              autocorrect: false,
                              enableSuggestions: false,
                              obscureText: true,
                              validator: viewModel.validatePassword,
                              onSaved: viewModel.savePassword,
                            ),
                            Row(
                              children: [
                                Expanded(child: SizedBox()),
                                TextButton(
                                  onPressed: viewModel.goToResetPassword,
                                  child: const Text.rich(
                                    TextSpan(
                                      text: "Forgot password? ",
                                      children: [
                                        TextSpan(
                                          text: "Reset",
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
                                  : const Text("SIGN IN"),
                            ),
                          ],
                        ),
                      ),
                    ),
                    Container(
                      margin: const EdgeInsets.symmetric(
                              horizontal: 50, vertical: 20)
                          .copyWith(top: 70),
                      child: const Text(
                        "There is no way to signup. You must be a Mensa member to sign in into this app.",
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
