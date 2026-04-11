package org.lime.core.common.services.cooldowns;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import org.lime.core.common.api.FieldFactory;
import org.lime.core.common.utils.Disposable;

import java.util.function.Function;

public class CooldownFieldFactory
        extends FieldFactory.AnnotatedGeneric<InjectCooldown> {
    public CooldownFieldFactory() {
        super(Cooldown.class, InjectCooldown.class);
    }

    @Override
    protected <T> Function<Disposable.Composite, T> create(InjectCooldown annotation, TypeLiteral<?> key, TypeEncounter<?> encounter) {
        var cooldownsProvider = encounter.getProvider(Cooldowns.class);
        //noinspection unchecked
        return composite -> (T)cooldownsProvider.get().group(key, annotation);
    }
}
