/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/**
 * Additions/edits made by Daniel Gregoire (semperos).
 */

package gershwin.lang;

import clojure.lang.AFn;
import clojure.lang.LispReader;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

public class Parser {
    // *************** Copied because they're not public in LispReader ***************
    /**
     * Allow traditional whitespace or commas to be considered whitespace
     * (following Clojure's convention).
     */
    static boolean isWhitespace(int ch) {
        return Character.isWhitespace(ch) || ch == ',';
    }

    /**
     * Put a character back in the {@link PushbackReader}
     */
    static void unread(PushbackReader r, int ch) {
	if(ch != -1) {
            try {
                r.unread(ch);
            } catch(IOException e) {
                throw Util.sneakyThrow(e);
            }
        }
    }
    // *************** End copying because things aren't public in LispReader ***************

    /**
     * Primary read method for Gershwin. Handle Gershwin-specific things, then
     * hand off to Clojure's reader.
     */
    public static Object read(PushbackReader r, boolean eofIsError, Object eofValue, boolean isRecursive) {
        try {
            for(; ;) {
                int ch = LispReader.read1(r);

                while(isWhitespace(ch))
                    ch = LispReader.read1(r);

                if(ch == -1) {
                    if(eofIsError)
                        throw Util.runtimeException("EOF while reading");
                    return eofValue;
                }

                /**** Gershwin extensions to Clojure reading ****/
                // Word definitions
                // This is what requires our PushbackReader to have a buffer size of 2.
                // A ':' could either be a word definition or the beginning of a Clojure keyword.
                // If ':' is followed by a space, it's a word definition, else it's handed off
                // to the Clojure reader.
                if(ch == ':') {
                    int ch2 = LispReader.read1(r);
                    if(isWhitespace(ch2)){
                        // System.out.println("Reading a Gershwin Word definition...");
                        unread(r, ch2);
                        return new ColonReader().invoke(r, (char) ch);
                    } else {
                        unread(r, ch2);
                    }
                }

                // We'll use '<' and '>' to contain quotations, since they're
                // one of the few characters that are left unreadable by the Clojure reader.
                if(ch == '<') {
                    return new QuotationReader().invoke(r, (char) ch);
                }

                // This is where Clojure does a check against all special macro
                // forms, invoking them and checking whether they return a meaningful
                // value or just the Reader. The comment is an example of one that
                // returns the Reader, so we just want to ignore it. Going to put
                // the extra checks in here as a reminder about the more maintainable
                // way of handling "macro" forms, into which ':' might also fall.
                if(ch == '!') {
                    Object ret = new CommentReader().invoke(r, (char) ch);
                    if(ret == r)
                        continue;
                    return ret;
                }

                /**** End Gershwin extensions to Clojure reading ****/
                // Everything else is just Clojure.
                // System.out.println("Clojure Reader => " + (char) ch);
                unread(r, ch);
                return LispReader.read(r, eofIsError, eofValue, isRecursive);
            }
        } catch (Exception e) {
            if(isRecursive || !(r instanceof LineNumberingPushbackReader))
                throw Util.sneakyThrow(e);
            LineNumberingPushbackReader rdr = (LineNumberingPushbackReader) r;
            throw new ReaderException(rdr.getLineNumber(), rdr.getColumnNumber(), e);
        }
    }

    public static class ColonReader extends AFn {
        public Object invoke(Object reader, Object colon) {
            PushbackReader r = (PushbackReader) reader;
            return new ColonList(LispReader.readDelimitedList(';', r, false));
        }
    }

    public static class QuotationReader extends AFn {
        public Object invoke(Object reader, Object colon) {
            PushbackReader r = (PushbackReader) reader;
            return new QuotationList(LispReader.readDelimitedList('>', r, false));
        }
    }

    public static class CommentReader extends AFn {
	public Object invoke(Object reader, Object bang) {
            Reader r = (Reader) reader;
            int ch;
            do {
                ch = LispReader.read1(r);
            } while(ch != -1 && ch != '\n' && ch != '\r');
            return r;
	}
    }

    /**
     * Included here because column and line have package-level visibility
     * in {@link LispReader}.
     */
    public static class ReaderException extends RuntimeException {
	final int line;
	final int column;

	public ReaderException(int line, int column, Throwable cause) {
            super(cause);
            this.line = line;
            this.column = column;
	}
    }

}
