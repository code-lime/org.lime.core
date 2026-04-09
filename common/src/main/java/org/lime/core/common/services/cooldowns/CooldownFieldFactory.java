package org.lime.core.common.services.cooldowns;

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import org.lime.core.common.api.FieldFactory;

public class CooldownFieldFactory
        extends FieldFactory.AnnotatedGeneric<InjectCooldown> {
    public CooldownFieldFactory() {
        super(Cooldown.class, InjectCooldown.class);
    }

    @Override
    protected <T> Provider<T> create(InjectCooldown annotation, TypeLiteral<?> key, TypeEncounter<?> encounter) {
        var cooldownsProvider = encounter.getProvider(Cooldowns.class);
        //noinspection unchecked
        return () -> (T)cooldownsProvider.get().group(key, annotation);
    }
}
