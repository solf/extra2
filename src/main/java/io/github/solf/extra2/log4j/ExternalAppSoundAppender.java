/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.log4j;



import static io.github.solf.extra2.util.NullUtil.nn;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import io.github.solf.extra2.console.Console;
import lombok.Getter;
import lombok.Setter;

/**
 * Appender that 'plays a sound' by running an external program with given argument.
 * Obviously can be used for non-sound purposes as well.
 * 
 * The reason for existence -- all (?) JVMs after 1.6.20 or so tend to hang
 * on me on Windows 7 64 bit when trying to use Applet or AudioSystem to play
 * a sound (hang as in JVM hang, not simply Java code issue). Doesn't happen
 * all the time, but relatively often.
 *
 * Not really tested much and performance is questionable, so use only for minor stuff.
 * 
 * Use a filter in combination with this appender to control when the appender
 * is triggered.
 * 
 * For example, in the appender definition, include a LevelMatchFilter
 * configured to accept WARN or greater, followed by a DenyAllFilter.
 * 
 * @author Sergey Olefir
 * 
 */
public final class ExternalAppSoundAppender extends AppenderSkeleton
{
	private String audioURL;
	/**
	 * sound player.
	 */
	private String soundPlayer;
	
	/**
	 * Minimum delay between two sounds being played by this instance -- to avoid
	 * potential out-of-memory crashes and other issues if too many messages
	 * 'in a row'. 
	 * <p>
	 * In milliseconds, default is 500
	 */
	@Getter @Setter
	private long minimumIntervalBetweenSounds = 500;
	
	/**
	 * Time last sound was played at.
	 */
	private final AtomicLong lastPlayedAt = new AtomicLong(0);
	
	/**
	 * Whether appender is ready for use.
	 */
	private boolean ready = false;

	public ExternalAppSoundAppender()
	{
	}

	/**
	 * Attempt to initialize the appender by creating a reference to an
	 * AudioClip.
	 * 
	 * Will log a message if format is not supported, file not found, etc.
	 * 
	 */
	@Override
	public void activateOptions()
	{
		ready = false;
		
		if (soundPlayer != null)
		{
			File file = new File(soundPlayer);
			if (file.exists() && file.isFile())
			{
				if (audioURL != null)
					ready = true;
				else
					LogLog.error("Unable to initialize SoundAppender: audioURL is not set");
			}
			else
				LogLog.error("Unable to initialize SoundAppender: soundPlayer is not a valid file: " + soundPlayer);
		}
		else
			LogLog.error("Unable to initialize SoundAppender: soundPlayer not set");
	}

	/**
	 * Accessor
	 * 
	 * @return audio file
	 */
	public String getAudioURL()
	{
		return audioURL;
	}

	/**
	 * Mutator - common format for a file-based url:
	 * file:///c:/path/someaudioclip.wav
	 * 
	 * @param audioURL
	 */
	public void setAudioURL(String audioURL)
	{
		this.audioURL = audioURL;
	}

	/**
	 * Play the sound if an event is being processed
	 */
	@Override
	protected void append(LoggingEvent event)
	{
		if (ready)
		{
			long now = System.currentTimeMillis();
			long limit = now - minimumIntervalBetweenSounds;
			long lastPlay = lastPlayedAt.get();
			if (lastPlay > limit)
				return;
			
			if (!lastPlayedAt.compareAndSet(lastPlay, now))
				return;
			
			try
			{
				Console.asyncRunExternal(".", false, nn(soundPlayer), nn(audioURL));
			} catch (Exception e)
			{
				// Ignore.
			}
		}
	}

	@Override
	public void close()
	{
		// nothing to do
	}

	/**
	 * Gets whether appender requires a layout.
	 * @return false
	 */
	@Override
	public boolean requiresLayout()
	{
		return false;
	}

	/**
	 * Gets sound player.
	 * @return sound player
	 */
	public String getSoundPlayer()
	{
		return soundPlayer;
	}

	/**
	 * Sets sound player.
	 * @param newSoundPlayer new value of sound player
	 */
	public void setSoundPlayer(String newSoundPlayer)
	{
		soundPlayer = newSoundPlayer;
	}

}
