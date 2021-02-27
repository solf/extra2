/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.email;

import static site.sonata.extra2.util.NullUtil.nn;
import static site.sonata.extra2.util.NullUtil.nonNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeUtility;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for supporting E-mail stuff.
 * 
 * NOTE: if logging level set to trace -- will output trace information such
 * as tracing e-mails being scanned.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class EmailUtil
{
	/**
	 * Mode for string matching.
	 */
	public static enum MatchMode
	{
		EXACT,
		SUBSTRING,
		/** String argument is considered to be a regexp to be matched against data */
		REGEXP,
	}
	
	/**
	 * Logger.
	 */
	private static final Logger log = nn(LoggerFactory.getLogger(EmailUtil.class));
	
	/**
	 * Open given IMAP store.
	 * 
	 * @throws ImapException if something goes wrong.
	 */
	public static Store openImapStore(String host, String user, String password)
		throws ImapException
	{
		return openImapStore("imap", host, -1, user, password);
	}
	
	/**
	 * Open given IMAP store.
	 * 
	 * @param protocol imap or imaps (for SSL), possibly something else is also
	 * 		supported
	 * @param port negative value means to use default
	 * 
	 * @throws ImapException if something goes wrong.
	 */
	public static Store openImapStore(String protocol, String host, int port, String user, String password)
		throws ImapException
	{
		assert !host.trim().isEmpty();
		assert !user.trim().isEmpty();
		assert !password.trim().isEmpty();
		
		Properties props = System.getProperties();
		// Another possible variation of the imap connection...
		// props.setProperty("mail.store.protocol", "imaps");
		// Store store = session.getStore("imaps");
	
		Session session = Session.getInstance(props, null);
//		session.setDebug(true);
		
		try
		{
			Store store = session.getStore(protocol);
			if (port >= 0)
				store.connect(host, port, user, password);
			else
				store.connect(host, user, password);
			return store;
		} catch (Exception e)
		{
			throw ImapException.wrap(e);
		}
	}
	
	/**
	 * Open specified folder in the imap store.
	 */
	public static Folder openReadOnlyFolder(Store store, String folderName)
		throws ImapException
	{
		assert store != null;
		assert !folderName.trim().isEmpty();
		
		try
		{
			Folder folder = store.getFolder(folderName);
			if (!folder.exists())
				throw new ImapException("Folder [" + folderName + "] doesn't exist in store: " + store);
			
			if (!folder.isOpen())
				folder.open(Folder.READ_ONLY);
			
			return folder;
		} catch (Exception e)
		{
			throw ImapException.wrap(e);
		}
	}
	
	
	/**
	 * Searches for specific mail message(s) in the given folder.
	 * Only mails that are not older than the given day are scanned.
	 * Folder must be open.
	 * 
	 * NOTE: 'from' search is made against individual addresses (which in theory
	 * there could be more than one (?) according to Java API). Address strings
	 * are formated like this:
	 * CM Data Warehouse <data@classmates.com>
	 *
	 * NOTE: if you want to search only by subject, you can give null 'from'.
	 * If you give null 'from', then value of 'from mode' doesn't matter -- can
	 * also be null. Similarly you can give null subject. You can also give nulls
	 * for all search criteria -- in which case you'll get list of all messages.  
	 * 
	 * @return found messages ordered from the oldest to the newest, possibly empty list, never null
	 * @throws ImapException 
	 */
	public static List<Message> findMessages(Folder folder, 
		@Nullable String fromString, @Nullable MatchMode fromMode, 
		@Nullable String subjectString, @Nullable MatchMode subjectMode, 
		DateTime oldestAllowed)
			throws ImapException
	{
		return findMessages(folder, fromString, fromMode, subjectString, subjectMode, oldestAllowed, false);
	}
		
	/**
	 * Searches for specific mail message(s) in the given folder.
	 * Only mails that are not older than the given day are scanned.
	 * Folder must be open.
	 * 
	 * NOTE: 'from' search is made against individual addresses (which in theory
	 * there could be more than one (?) according to Java API). Address strings
	 * are formated like this:
	 * CM Data Warehouse <data@classmates.com>
	 *
	 * NOTE: if you want to search only by subject, you can give null 'from'.
	 * If you give null 'from', then value of 'from mode' doesn't matter -- can
	 * also be null. Similarly you can give null subject. You can also give nulls
	 * for all search criteria -- in which case you'll get list of all messages.  
	 * 
	 * @return found messages ordered from the oldest to the newest, possibly empty list, never null
	 * @throws ImapException 
	 */
	private static List<Message> findMessages(Folder folder, 
		@Nullable String fromString, @Nullable MatchMode fromMode, 
		@Nullable String subjectString, @Nullable MatchMode subjectMode, 
		DateTime oldestAllowed, boolean oneMessageOnly)
			throws ImapException, IllegalArgumentException
	{
		assert folder != null;
		assert oldestAllowed != null;
		
		if (!folder.isOpen())
			throw new ImapException("Folder is not open: " + folder);
		
		try
		{
			// Result.
			List<Message> result = new ArrayList<Message>();
			
			// Handle 'null' (any) searches.
			if (fromString == null)
			{
				fromString = "";
				fromMode = MatchMode.SUBSTRING;
			}
			else if (fromMode == null)
				throw new IllegalArgumentException("fromMode may not be null when fromString isn't null.");
			
			if (subjectString == null)
			{
				subjectString = "";
				subjectMode = MatchMode.SUBSTRING;
			}
			else if (subjectMode == null)
				throw new IllegalArgumentException("subjectMode may not be null when subjectString isn't null.");
			
			Pattern subjectPattern = null;
			switch(subjectMode)
			{
				case EXACT:
				case SUBSTRING:
					break;
				case REGEXP:
					subjectPattern = Pattern.compile(subjectString);
					break;
			}
			
			Pattern fromPattern = null;
			switch(fromMode)
			{
				case EXACT:
				case SUBSTRING:
					break;
				case REGEXP:
					fromPattern = Pattern.compile(fromString);
					break;
			}
			
			
			for (int i = folder.getMessageCount(); i > 0; i--)
			{
				Message msg = folder.getMessage(i);
				DateTime sentOn = new DateTime(nonNull(msg.getSentDate()));
				if (sentOn.isBefore(oldestAllowed))
					break;
				
				boolean match = false;
				
				// Match from.
				String adrString = null;
				for (Address address : msg.getFrom())
				{
					adrString = address.toString();
					
					switch(fromMode)
					{
						case EXACT:
							match = adrString.equals(fromString);
							break;
						case SUBSTRING:
							match = adrString.contains(fromString);
							break;
						case REGEXP:
							assert fromPattern != null;
							match = fromPattern.matcher(adrString).matches();
							break;
					}
					
					if (match)
						break;
				}
				
				if (match)
				{
					// Match subject.
					String msgSubject = msg.getSubject();
					if (msgSubject == null)
						msgSubject = ""; // Can happen with empty subject.
					switch(subjectMode)
					{
						case EXACT:
							match = subjectString.equals(msgSubject);
							break;
						case SUBSTRING:
							match = msgSubject.contains(subjectString);
							break;
						case REGEXP:
							assert subjectPattern != null;
							match = subjectPattern.matcher(msgSubject).matches();
							break;
					}
				}
				
				if (match)
				{
					result.add(0, msg); // To sort list properly, although at a performance cost.
					
					if (log.isTraceEnabled()) log.trace("[match] scanning message sent on [{}] from [{}]", msg.getSentDate(), adrString);
					
					if (oneMessageOnly)
						break;
				}
				else
				{
					if (log.isTraceEnabled()) log.trace("[     ] scanning message sent on [{}] from [{}]", msg.getSentDate(), adrString);
				}
			}
			
			return result;
		} catch (Exception e)
		{
			throw ImapException.wrap(e);
		}
	}
	
	
	/**
	 * Searches for newest mail message in the given folder that matches criteria
	 * given.
	 * Only mails that are not older than the given day are scanned.
	 * Folder must be open.
	 * 
	 * NOTE: 'from' search is made against individual addresses (which in theory
	 * there could be more than one (?) according to Java API). Address strings
	 * are formated like this:
	 * CM Data Warehouse <data@classmates.com>
	 *
	 * NOTE: if you want to search only by subject, you can give null 'from'.
	 * If you give null 'from', then value of 'from mode' doesn't matter -- can
	 * also be null. Similarly you can give null subject. You can also give nulls
	 * for all search criteria -- in which case you'll get list of all messages.  
	 * 
	 * @return found message or null if none found
	 * @throws ImapException 
	 */
	@Nullable
	public static Message findNewestMessage(Folder folder, 
		@Nullable String fromString, @Nullable MatchMode fromMode, 
		@Nullable String subjectString, @Nullable MatchMode subjectMode, 
		DateTime oldestAllowed)
			throws ImapException
	{
		List<Message> msgs = findMessages(folder, fromString, fromMode, subjectString, subjectMode, oldestAllowed, true);
		
		if (msgs.size() == 0)
			return null;
		
		assert msgs.size() == 1;
		return msgs.get(0);
	}
	
	/**
	 * Gets specific attachment from the message.
	 * 
	 * @throws ImapExceptionif something goes wrong
	 * @throws IllegalArgumentException if matching attachment not found
	 */
	public static InputStream findAttachment(Message msg, String attachmentFileName, MatchMode matchMode)
		throws ImapException, IllegalArgumentException
	{
		assert msg != null;
		assert attachmentFileName != null;
		assert matchMode != null;

		Pattern attachmentPattern = null;
		switch(matchMode)
		{
			case EXACT:
			case SUBSTRING:
				break;
			case REGEXP:
				attachmentPattern = Pattern.compile(attachmentFileName);
				break;
		}
		
		String msgString = msg.toString();
		try
		{
			msgString = msg.getSentDate() + ": " + msg.getSubject(); // This could throw exception, so inside try/catch
			
			Multipart mp = (Multipart)msg.getContent();
			
			// Scan all parts looking for our attachment.
			// Skip first part (assuming message text).
			for (int i = 1; i < mp.getCount(); i++)
			{
				BodyPart part = mp.getBodyPart(i);
				String disposition = part.getDisposition();
				// Don't ask -- for whatever reason null disposition for non-first
				// part means attachment in some cases.
//				System.out.println("DISPOSITION: " + disposition); {}
				if ((disposition == null) || Part.ATTACHMENT.equalsIgnoreCase(disposition))
				{
					// Found attachment, see if it matches.
					String fileName = MimeUtility.decodeText(part.getFileName());
//					System.out.println("ATTACHMENT: " + fileName); {}
					boolean match = false;
					switch(matchMode)
					{
						case EXACT:
							match = fileName.equals(attachmentFileName);
							break;
						case SUBSTRING:
							match = fileName.contains(attachmentFileName);
							break;
						case REGEXP:
							assert attachmentPattern != null;
							match = attachmentPattern.matcher(fileName).matches();
							break;
					}
					if (match)
						return nonNull(part.getInputStream());
				}
			}
		} catch (Exception e)
		{
			throw ImapException.wrap(e);
		}
		
		throw new IllegalArgumentException("Attachment [" + attachmentFileName + "] not found in message: " + msgString);
	}
}
