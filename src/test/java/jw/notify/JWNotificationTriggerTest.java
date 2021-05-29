package jw.notify;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.jupiter.api.Test;

public class JWNotificationTriggerTest {
	@Test
	public void trigger() throws AddressException, InterruptedException, IOException, MessagingException {
		Main.main(null);
	}
}
