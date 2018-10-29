/**
 * @author feiyang.d
 * @date 2018/10/26
 */
public class Test {
    public static void main(String [] args){
        Hello hello2 = RPCProxy.create(new HelloImpl());
        System.out.println(hello2.print("world!"));
    }
}
