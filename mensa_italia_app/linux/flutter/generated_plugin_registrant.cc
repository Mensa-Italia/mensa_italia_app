//
//  Generated file. Do not edit.
//

// clang-format off

#include "generated_plugin_registrant.h"

#include <validation_pro/validation_pro_plugin.h>

void fl_register_plugins(FlPluginRegistry* registry) {
  g_autoptr(FlPluginRegistrar) validation_pro_registrar =
      fl_plugin_registry_get_registrar_for_plugin(registry, "ValidationProPlugin");
  validation_pro_plugin_register_with_registrar(validation_pro_registrar);
}
