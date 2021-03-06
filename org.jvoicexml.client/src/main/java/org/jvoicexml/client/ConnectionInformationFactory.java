/*
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2012-2017 JVoiceXML group - http://jvoicexml.sourceforge.net
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

package org.jvoicexml.client;


/**
 * A factory or {@link org.jvoicexml.ConnectionInformation}.
 * @author Dirk Schnelle-Walka
 * @since 0.7.6
 */
public interface ConnectionInformationFactory {
    /**
     * Retrieves all identifiers for {@link org.jvoicexml.CallControl} that are
     * supported by this factory.
     * @return all identifiers.
     */
    String[] getCallControlIdentifiers();

    /**
     * Retrieves all identifiers for {@link org.jvoicexml.SystemOutput} that are
     * supported by this factory.
     * @return all identifiers.
     */
    String[] getSystemOutputIdentifiers();

    /**
     * Retrieves all identifiers for {@link org.jvoicexml.UserInput} that are
     * supported by this factory.
     * @return all identifiers.
     */
    String[] getUserInputIdentifiers();

    /**
     * Creates a controller for a {@link org.jvoicexml.ConnectionInformation}
     * with the specified identifiers.
     * @param call unique identifier for the {@link org.jvoicexml.CallControl}.
     * @param output unique identifier for the
     *  {@link org.jvoicexml.SystemOutput}.
     * @param input unique identifier for the {@link org.jvoicexml.UserInput}.
     * @return created connection information
     * @exception UnsupportedResourceIdentifierException
     *           if one of the specified identifiers is not supported in
     *           this factory
     */
    ConnectionInformationController createConnectionInformation(
            String call, String output, String input)
                    throws UnsupportedResourceIdentifierException;
}
