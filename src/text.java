public class text {
    String str="best";
    char[] ch={'n','o','o','d'};

    public  void change(String str,char[] ch){
        this.str="hello";
        ch[0]='g';
    }




    public static void main(String[] args) {
//        System.out.println("hello");
        text a=new text();
        a.change(a.str,a.ch);
        System.out.println(a.str);
        System.out.println(a.ch);



    }
}
