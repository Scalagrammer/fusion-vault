package scg.fusion.vault;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.Logical;
import scg.fusion.AutowiringHook;
import scg.fusion.ComponentFactory;
import scg.fusion.Environment;
import scg.fusion.Joint;

import scg.fusion.annotation.Initialize;
import scg.fusion.annotation.Utilize;
import scg.fusion.vault.annotation.Secret;

import java.util.*;

public final class VaultUnit {

    public static final String FUSION_VAULT_ADDRESS_PROPERTY_NAME = "fusion.vault.address";

    private final ComponentFactory components;

    private final List<Destroyable> allSecrets = new ArrayList<>();

    private VaultUnit(ComponentFactory components) {
        this.components = components;
    }

    @Initialize
    private void autowireSecrets() throws VaultException {

        Map<Joint, AutowiringHook> secrets = components.autowiringBy("@autowire(%s)", Secret.class);

        Logical vault = initializeVault(components.get(Environment.class));

        for (Joint joint : secrets.keySet()) {

            Secret secret = joint.getAnnotation(Secret.class);

            String secretName = secret.name();

            String secretPath = secret.path();

            int secretVersion = secret.version();

            Map<String, String> data = vault.read(secretPath, true, secretVersion).getData();

            if (data.containsKey(secretName)) {

                String secretValue = data.get(secretName);

                DestroyableImpl destroyable = new DestroyableImpl(secretValue);

                secrets.get(joint).autowireWith(components.swap(destroyable));

                allSecrets.add(destroyable);

            } else {
                throw new SecretNotFoundException("name [%s], version [%d], path [%s]", secretName, secretVersion, secretPath);
            }
        }
    }

    @Utilize
    private void destroy() {
        Collections.reverse(allSecrets);
        allSecrets.forEach(Destroyable::close);
    }

    private static Logical initializeVault(Environment environment) throws VaultException {
        return new Vault(initializeVaultConfig(environment)).logical();
    }

    private static VaultConfig initializeVaultConfig(Environment environment) throws VaultException {

        VaultConfig vaultConfig = new VaultConfig();

        if (environment.hasProperty(FUSION_VAULT_ADDRESS_PROPERTY_NAME)) {
            vaultConfig.address(environment.getProperty(FUSION_VAULT_ADDRESS_PROPERTY_NAME));
        }

        if (environment.hasProperty("fusion.vault.namespace")) {
            vaultConfig.nameSpace(environment.getProperty("fusion.vault.namespace"));
        }

        if (environment.hasProperty("fusion.vault.token")) {
            vaultConfig.token(environment.getProperty("fusion.vault.token"));
        }

        if (environment.hasProperty("fusion.vault.read.timeout")) {
            vaultConfig.readTimeout(environment.getInt("fusion.vault.read.timeout"));
        }

        if (environment.hasProperty("fusion.vault.open.timeout")) {
            vaultConfig.openTimeout(environment.getInt("fusion.vault.open.timeout"));
        }

        if (environment.hasProperty("fusion.vault.path.prefix")) {
            vaultConfig.prefixPath(environment.getProperty("fusion.vault.path.prefix"));
        }

        if (environment.hasProperty("fusion.vault.engine.version")) {
            vaultConfig.engineVersion(environment.getInt("fusion.vault.engine.version"));
        }

        if (environment.hasProperty("fusion.vault.ssl")) {
            if (environment.getBoolean("fusion.vault.ssl")) {

                SslConfig sslConfig = new SslConfig();

                if (environment.hasProperty("fusion.vault.ssl.pem.utf8")) {
                    sslConfig.pemUTF8(environment.getProperty("fusion.vault.ssl.pem.utf8"));
                }

                if (environment.hasProperty("fusion.vault.ssl.pem.resource")) {
                    sslConfig.pemResource(environment.getProperty("fusion.vault.ssl.pem.resource"));
                }

                if (environment.hasProperty("fusion.vault.ssl.client.pem.resource")) {
                    sslConfig.clientPemResource(environment.getProperty("fusion.vault.ssl.client.pem.resource"));
                }

                if (environment.hasProperty("fusion.vault.ssl.client.pem.utf8")) {
                    sslConfig.clientPemUTF8(environment.getProperty("fusion.vault.ssl.client.pem.utf8"));
                }

                if (environment.hasProperty("fusion.vault.ssl.client.key.pem.utf8")) {
                    sslConfig.clientKeyPemUTF8(environment.getProperty("fusion.vault.ssl.client.key.pem.utf8"));
                }

                if (environment.hasProperty("fusion.vault.ssl.client.key.pem.resource")) {
                    sslConfig.clientKeyPemResource(environment.getProperty("fusion.vault.ssl.client.key.pem.resource"));
                }

                if (environment.hasProperty("fusion.vault.ssl.trust.store.resource")) {
                    sslConfig.trustStoreResource(environment.getProperty("fusion.vault.ssl.trust.store.resource"));
                }

                vaultConfig.sslConfig(sslConfig);
            }
        }

        return vaultConfig.build();
    }

}
