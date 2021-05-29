package jw.notify;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

public class Main {
	private static final Properties PROPERTIES = new Properties();

	private static final String NO_NEW_DOCUMENTS_MESSAGE = "Seit der letzten Überprüfung aktualisierte Dokumente:\n"
			+ "Keine. Es sind keine Dokumente hinzugefügt oder geändert worden.";

	WebDriver driver;

	public static void main(String[] args)
			throws InterruptedException, IOException, AddressException, MessagingException {
		PROPERTIES.load(new FileInputStream("jw-notify.properties"));
		try {
			Main main = new Main();
			AtomicBoolean success = new AtomicBoolean(true);
			main.getDocumentsIfUnread().ifPresent(updatedDocuments -> {
				try {
					sendMail(PROPERTIES.getProperty("mail.recipient"), PROPERTIES.getProperty("mail.subject"),
							PROPERTIES.getProperty("mail.prefix") + StringEscapeUtils.escapeHtml4(updatedDocuments));
				} catch (MessagingException e) {
					success.set(false);
					e.printStackTrace();
				}
			});
			if (success.get()) {
				new URL(PROPERTIES.getProperty("healthcheck")).getContent();
			}
		} catch (Exception e) {
			e.printStackTrace();
			sendMail(PROPERTIES.getProperty("mail.recipient"), "Fehler in JW-Notify",
					e.getClass() + ": " + e.getMessage());
		}
		// Instantiate Web Driver
	}

	public static void sendMail(String recipient, String subject, String text)
			throws AddressException, MessagingException {
		Properties prop = new Properties();
		prop.put("mail.smtp.auth", true);
		prop.put("mail.smtp.starttls.enable", "true");
		prop.put("mail.smtp.host", PROPERTIES.getProperty("mail.smtp.host"));
		prop.put("mail.smtp.port", PROPERTIES.getProperty("mail.smtp.port"));
		prop.put("mail.smtp.ssl.trust", PROPERTIES.getProperty("mail.smtp.host"));

		Session session = Session.getInstance(prop, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(PROPERTIES.getProperty("mail.sender"),
						PROPERTIES.getProperty("mail.smtp.password"));
			}
		});

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(PROPERTIES.getProperty("mail.sender")));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
		message.setSubject(subject);

		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(text, "text/html");

		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(mimeBodyPart);

		message.setContent(multipart);

		Transport.send(message);
	}

	public Main() throws IOException {
//		URL resource = Main.class.getResource("/resources/geckodriver");
//		File f = new File("Driver");
//		if (!f.exists()) {
//			f.mkdirs();
//		}
//		File firefoxDriver = new File("Driver" + File.separator + "geckodriver");
//		if (!firefoxDriver.exists() || !firefoxDriver.canExecute()) {
//			firefoxDriver.createNewFile();
//			FileUtils.copyURLToFile(resource, firefoxDriver);
//			firefoxDriver.setExecutable(true);
//		}
//
//		System.setProperty("webdriver.gecko.driver", firefoxDriver.getAbsolutePath());
		WebDriverManager.chromedriver().setup();

		ChromeOptions options = new ChromeOptions();
		options.setHeadless(true);

		driver = new ChromeDriver(options);
	}

	public Optional<String> getDocumentsIfUnread() throws IOException {
		String updatedDocuments = getUpdatedDocuments();
		File lastMessageFile = new File("lastMessage");
		if (NO_NEW_DOCUMENTS_MESSAGE.equals(updatedDocuments) || (lastMessageFile.exists()
				&& IOUtils.toString(lastMessageFile.toURI(), "UTF-8").equals(updatedDocuments))) {
			return Optional.empty();
		}
		IOUtils.write(updatedDocuments, new FileOutputStream("lastMessage"), "UTF-8");
		return Optional.of(updatedDocuments);
	}

	private String getUpdatedDocuments() {
		driver.get(PROPERTIES.getProperty("url"));

		// Cookie-Dialog
		getElementWhenClickable(By.className("legal-notices-client--accept-button")).click();

		getElementWhenClickable(By.id("form.userName")).sendKeys(PROPERTIES.getProperty("user"));
		getElementWhenClickable(By.cssSelector("button[type=submit]")).click();

		getElementWhenClickable(By.id("passwordInput")).sendKeys(PROPERTIES.getProperty("password"));
		getElementWhenClickable(By.id("submitButton")).click();

		getElementWhenClickable(By.className("field__input--password")).sendKeys(PROPERTIES.getProperty("answer1"));
		driver.findElements(By.className("field__input--password")).get(1).sendKeys(PROPERTIES.getProperty("answer2"));
		getElementWhenClickable(By.cssSelector("button[type=submit]")).click();

		getElementWhenClickable(By.xpath("//*[contains(text(), 'Dokumente')]")).click();

		getElementWhen(
				ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(By.tagName("unread-documents")),
						(ExpectedConditions.invisibilityOfElementLocated(By.className("loading")))));

		String unreadDocuments = getElementWhen(
				ExpectedConditions.visibilityOfElementLocated(By.tagName("unread-documents"))).getText();

		driver.close();

		System.out.println(unreadDocuments);

		return unreadDocuments;
	}

	private WebElement getElementWhenClickable(By by) {
		return getElementWhen(ExpectedConditions.elementToBeClickable(by));
	}

	private <T> T getElementWhen(ExpectedCondition<T> elementToBeClickable) {
		return new WebDriverWait(driver, 30).until(elementToBeClickable);
	}
}
