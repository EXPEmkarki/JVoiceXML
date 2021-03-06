/*
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2006-2017 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.jvoicexml;

/**
 * Facade for easy control and monitoring of the user's DTMF input.
 *
 * <p>
 * Objects that implement this interface are able to detect character input and
 * to control input detection interval duration with a timer whose length is
 * specified by a VoiceXML document.
 * </p>
 *
 * <p>
 * If an input resource is not available, an <code>error.noresource</code> event
 * must be thrown.
 * </p>
 *
 * @author Dirk Schnelle-Walka
 * @since 0.5
 */
public interface DtmfInput {
    /**
     * The user entered a DTMF.
     *
     * @param dtmf
     *            Entered DTMF.
     * @throws IllegalArgumentException
     *             if the char is not one of 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, #, *.
     */
    void addDtmf(char dtmf) throws IllegalArgumentException;
}
