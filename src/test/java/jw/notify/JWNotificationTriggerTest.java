package jw.notify;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.junit.jupiter.api.Test;

public class JWNotificationTriggerTest {

	@Test
	public void hello() {
		System.out.println("lol");
		assertTrue(1 == 2 - 1);
	}

	@Test
	public void trigger() throws AddressException, InterruptedException, IOException, MessagingException {
		Main.main(null);
	}

}
