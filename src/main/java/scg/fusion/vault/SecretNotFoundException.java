package scg.fusion.vault;

import scg.fusion.exceptions.FusionRuntimeException;

public final class SecretNotFoundException extends FusionRuntimeException {
    SecretNotFoundException(String message, Object...args) {
        super(String.format(message, args));
    }
}
