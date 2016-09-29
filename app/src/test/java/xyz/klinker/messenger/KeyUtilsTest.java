package xyz.klinker.messenger;

import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import xyz.klinker.messenger.encryption.KeyUtils;

public class KeyUtilsTest extends MessengerSuite {

    private final String SALT = "abcdefghijklmnopqrstuvwxyz";
    private final String SALT2 = "zyxwvutsrqponmlkjihgfedcba";
    private final String accountId = "aabss-fjfj11-jfjf00-jj";

    @Test
    public void shouldAlwaysReturnTheSameHash() {
        String password = "pass1234";
        String firstHash = new KeyUtils().hashPassword(password, SALT);
        testHash(password, firstHash);
    }

    @Test
    public void shouldHashTheSameWhenPasswordHasSpaces() {
        String password = "pass 1234";
        String firstHash = new KeyUtils().hashPassword(password, SALT);
        testHash(password, firstHash);
    }

    @Test
    public void shouldHashTheSameWhenPasswordHasWeirdCharacters() {
        String password = "!@#$%^&*()[]{}|;:?><,.`~=-_+";
        String firstHash = new KeyUtils().hashPassword(password, SALT);
        testHash(password, firstHash);
    }

    @Test
    public void shouldHashTheSameWhenPasswordHasReturns() {
        String password = "test\n    password";
        String firstHash = new KeyUtils().hashPassword(password, SALT);
        testHash(password, firstHash);
    }

    @Test
    public void shouldCreateTheSameKey() {
        String password = "password";
        String hash = new KeyUtils().hashPassword(password, SALT);
        SecretKey key = new KeyUtils().createKey(hash, accountId, SALT2);
        testKey(hash, key);
    }

    @Test
    public void shouldCreateTheSameKeyWithSpacesInPassword() {
        String password = "password testing again";
        String hash = new KeyUtils().hashPassword(password, SALT);
        SecretKey key = new KeyUtils().createKey(hash, accountId, SALT2);
        testKey(hash, key);
    }

    @Test
    public void shouldCreateTheSameKeyWithSpecialCharsInPassword() {
        String password = "!@#$%^&*()[]{}|;:?><,.`~=-_+";
        String hash = new KeyUtils().hashPassword(password, SALT);
        SecretKey key = new KeyUtils().createKey(hash, accountId, SALT2);
        testKey(hash, key);
    }

    @Test
    public void shouldCreateTheSameKeyWithReturnsInPassword() {
        String password = "test\n    password";
        String hash = new KeyUtils().hashPassword(password, SALT);
        SecretKey key = new KeyUtils().createKey(hash, accountId, SALT2);
        testKey(hash, key);
    }

    @Test
    public void shouldCreateDifferentKeyWithDifferentPasswords() {
        String password = "!@#$%^&*()[]{}|;:?><,.`~=-_+";
        String hash = new KeyUtils().hashPassword(password, SALT);
        SecretKey key = new KeyUtils().createKey(hash, accountId, SALT2);

        String password2 = "!@#$%^&*()[]{}|;:";
        String hash2 = new KeyUtils().hashPassword(password2, SALT);
        SecretKey key2 = new KeyUtils().createKey(hash2, accountId, SALT2);

        assertFalse(key.equals(key2));
    }

    @Test
    public void shouldCreateDifferentKeyWithDifferentHashes() {
        String password = "!@#$%^&*()[]{}|;:?><,.`~=-_+";
        String hash = new KeyUtils().hashPassword(password, SALT);
        SecretKey key = new KeyUtils().createKey(hash, accountId, SALT2);

        String password2 = "!@#$%^&*()[]{}|;:";
        SecretKey key2 = new KeyUtils().createKey("testing a weird hash", accountId, SALT2);

        assertFalse(key.equals(key2));
    }

    @Test
    public void shouldCreateDifferentKeyWithDifferentAccountIds() {
        String password = "!@#$%^&*()[]{}|;:?><,.`~=-_+";
        String hash = new KeyUtils().hashPassword(password, SALT);
        SecretKey key = new KeyUtils().createKey(hash, accountId, SALT2);

        String password2 = "!@#$%^&*()[]{}|;:?><,.`~=-_+";
        String hash2 = new KeyUtils().hashPassword(password2, SALT);
        SecretKey key2 = new KeyUtils().createKey(hash2, "test account", SALT2);

        assertFalse(key.equals(key2));
    }

    @Test
    public void shouldCreateDifferentKeyWithDifferentSalts() {
        String password = "!@#$%^&*()[]{}|;:?><,.`~=-_+";
        String hash = new KeyUtils().hashPassword(password, SALT);
        SecretKey key = new KeyUtils().createKey(hash, accountId, SALT2);

        String password2 = "!@#$%^&*()[]{}|;:?><,.`~=-_+";
        String hash2 = new KeyUtils().hashPassword(password2, SALT);
        SecretKey key2 = new KeyUtils().createKey(hash2, accountId, "hey salt");

        assertFalse(key.equals(key2));
    }

    private void testHash(String password, String firstHash) {
        for (int i = 0; i < 10; i++) {
            assertTrue(firstHash.equals(new KeyUtils().hashPassword(password, SALT)));
        }
    }

    private void testKey(String hash, SecretKey key) {
        for (int i = 0; i < 10; i++) {
            assertTrue(key.equals(new KeyUtils().createKey(hash, accountId, SALT2)));
        }
    }
}
