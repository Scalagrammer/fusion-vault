package scg.fusion.vault;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;
import static java.util.function.Function.identity;

public interface Destroyable extends AutoCloseable {
    @Override
    void close();

    <R> R apply(Function<String, R> f);

    default boolean isDestroyed() {
        return isNull(raw());
    }

    default String raw() {
        return apply(identity());
    }
}

final class DestroyableImpl implements Destroyable {

    private static final AtomicReferenceFieldUpdater<DestroyableImpl, String> valueUpdater = newUpdater(DestroyableImpl.class, String.class, "value");

    private volatile String value;

    DestroyableImpl(String value) {
        this.value = value;
    }

    @Override
    public <R> R apply(Function<String, R> f) {
        return f.apply(value);
    }

    @Override
    public void close() {
        valueUpdater.set(this, (null));
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DestroyableImpl encoded = (DestroyableImpl) o;

        return value.equals(encoded.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
