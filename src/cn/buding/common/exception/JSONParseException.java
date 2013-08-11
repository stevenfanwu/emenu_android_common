package cn.buding.common.exception;

public class JSONParseException extends CustomException {
	private static final long serialVersionUID = -1130323416137119245L;

	public JSONParseException(String msg){
		super(msg);
	}
	
	public JSONParseException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public JSONParseException(Throwable cause) {
		super(cause);
	}
	

	public JSONParseException(int code, String msg, Throwable cause) {
		super(code, msg, cause);
	}

	public JSONParseException(int code) {
		super(code);
	}
	
	public JSONParseException(int code, Throwable cause){
		super(code, cause);
	}

	public JSONParseException(int code, String msg) {
		super(code, msg);
	}
}
