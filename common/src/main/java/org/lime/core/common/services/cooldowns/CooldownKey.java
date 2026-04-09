package org.lime.core.common.services.cooldowns;

import com.google.inject.TypeLiteral;

record CooldownKey<Key>(String group, TypeLiteral<Key> keyType) {
}
