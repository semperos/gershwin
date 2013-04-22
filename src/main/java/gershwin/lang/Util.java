package gershwin.lang;

public class Util {

    static public RuntimeException runtimeException(String s) {
	return new RuntimeException(s);
    }

    static public RuntimeException runtimeException(String s, Throwable e) {
	return new RuntimeException(s, e);
    }

    /**
     * Throw even checked exceptions without being required
     * to declare them or catch them. Suggested idiom:
     * <p>
     * <code>throw sneakyThrow( some exception );</code>
     */
    static public RuntimeException sneakyThrow(Throwable t) {
        // http://www.mail-archive.com/javaposse@googlegroups.com/msg05984.html
	if (t == null)
            throw new NullPointerException();
	Util.<RuntimeException>sneakyThrow0(t);
	return null;
    }

    @SuppressWarnings("unchecked")
    static private <T extends Throwable> void sneakyThrow0(Throwable t) throws T {
	throw (T) t;
    }

}
