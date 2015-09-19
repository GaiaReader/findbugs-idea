/*
 * Copyright 2008-2015 Andre Pfeiler
 *
 * This file is part of FindBugs-IDEA.
 *
 * FindBugs-IDEA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FindBugs-IDEA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FindBugs-IDEA.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.twodividedbyzero.idea.findbugs.core;


import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.twodividedbyzero.idea.findbugs.common.util.ErrorUtil;
import org.twodividedbyzero.idea.findbugs.common.util.FindBugsUtil;

import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


/**
 * @author Reto Merz<reto.merz@gmail.com>
 * @since 0.9.998
 */
public final class ErrorReportSubmitterImpl extends ErrorReportSubmitter {


	@Override
	public String getReportActionText() {
		return "Copy error to clipboard and open page";
	}


	@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
	@Override
	public boolean submit(@NotNull IdeaLoggingEvent[] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<SubmittedReportInfo> consumer) {
		final StringBuilder body = new StringBuilder();
		if (!StringUtil.isEmptyOrSpaces(additionalInfo)) {
			body.append(additionalInfo);
		}

		boolean isFindBugsError = true;
		String title = null;
		for (final IdeaLoggingEvent event : events) {
			final String message = event.getMessage();
			final boolean hasMessage = !StringUtil.isEmptyOrSpaces(message);
			final String stack = event.getThrowableText();
			final boolean hasStack = !StringUtil.isEmptyOrSpaces(stack);
			if (title == null && hasMessage) {
				title = message;
			}
			if (hasMessage || hasStack) {
				if (body.length() > 0) {
					body.append("\n\n");
				}
				if (hasMessage) {
					body.append(message);
					if (hasStack) {
						body.append("\n");
					}
				}
				if (hasStack) {
					body.append(stack);
				}
			}
			if (!hasStack) {
				isFindBugsError = false;
			} else if (!FindBugsUtil.isFindBugsError(event.getThrowable())) {
				isFindBugsError= false;
			}
		}
		if (title == null) {
			title = "Analysis Error";
		}

		//noinspection ConstantConditions
		return submitImpl(
				isFindBugsError,
				title,
				body.toString(),
				parentComponent,
				consumer
		);
	}


	private boolean submitImpl(
			final boolean isFindBugsError,
			@NotNull final String title,
			@NotNull final String errorText,
			@NotNull final Component parentComponent,
			@NotNull final Consumer<SubmittedReportInfo> consumer
	) {

		final StringBuilder url = new StringBuilder();
		if (isFindBugsError) {
			// http://sourceforge.net/p/findbugs/bugs/
			// is locked - assume we should report on github - 19.9.2015
			url.append("https://github.com/findbugsproject/findbugs");
		} else {
			url.append("https://github.com/andrepdo/findbugs-idea");
		}

		/**
		 * Note: set errorText as body does not work:
		 *   - can cause HTTP 414 Request URI too long
		 *   - if user is not yet logged in github login page will show an error
		 *     502 - "This page is taking way too long to load." (this will also occure with HTTP POST).
		 */
		final String body = "The error was copied to the clipboard. Press " + (SystemInfo.isMac ? "Command+V" : "Ctrl+V");
		url.append("/issues/new?title=").append(encode(title)).append("&body=").append(encode(body));

		CopyPasteManager.getInstance().setContents(new StringSelection(errorText));
		BrowserUtil.browse(url.toString());
		return true;
	}


	@NotNull
	private static String encode(@NotNull final String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw ErrorUtil.toUnchecked(e); // all system support UTF-8
		}
	}
}