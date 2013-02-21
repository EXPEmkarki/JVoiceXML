/*
 * File:    $RCSfile: JsgfGrammarTransformer.java,v $
 * Version: $Revision: 1.1 $
 * Date:    $Date: 2006/01/12 14:37:55 $
 * Author:  $Author: schnelle $
 * State:   $State: Exp $
 *
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2005 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jvoicexml.interpreter.grammar.transformer;

import java.io.StringReader;

import javax.speech.recognition.RuleGrammar;

import org.jvoicexml.event.error.BadFetchError;
import org.jvoicexml.event.error.NoresourceError;
import org.jvoicexml.event.error.UnsupportedFormatError;
import org.jvoicexml.implementation.UserInput;
import org.jvoicexml.interpreter.grammar.GrammarTransformer;

/**
 * This class implements the GrammarTransformer interface. An instance
 * of this class is able to transform a SRGS grammar with XML format
 * into RuleGrammar instance. The mime type of the accepted grammar is
 * application/x-jsgf.
 *
 * @author Christoph Buente
 * @author Dirk Schnelle
 *
 * @version $Revision: 1.1 $
 *
 * <p>
 * Copyright &copy; 2005 JVoiceXML group - <a
 * href="http://jvoicexml.sourceforge.net">http://jvoicexml.sourceforge.net/
 * </a>
 * </p>
 */
public final class JsgfGrammarTransformer
        implements GrammarTransformer {
    /** Transformer's type. */
    private static final String TYPE = "application/x-jsgf";

    /**
     * {@inheritDoc}
     */
    public RuleGrammar createGrammar(final UserInput input,
                                     final String grammar, final String type)
            throws NoresourceError, UnsupportedFormatError, BadFetchError {
        if (!TYPE.equals(type)) {
            throw new UnsupportedFormatError();
        }

        final StringReader reader = new StringReader(grammar);

        return input.loadGrammar(reader);
    }

    /**
     * {@inheritDoc}
     */
    public String getSupportedType() {
        return TYPE;
    }

}
