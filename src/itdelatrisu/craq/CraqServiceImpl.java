package itdelatrisu.craq;

import org.apache.thrift.TException;

/** CRAQ service implementation. */
public class CraqServiceImpl implements CraqService.Iface {
	@Override
	public int add(int number1, int number2) throws TException {
		return number1 + number2;
	}
}
