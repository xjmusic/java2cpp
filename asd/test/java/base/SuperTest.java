package base;


public class SuperTest{

  int val;

  void print(){};

  static class inner extends SuperTest{
    void print(){};
  
  
    void print2(){
      print();//this->print()
      super.print();//SuperTest::print
      this.print2();//this->print2()
    }
    
    
    static class inner2 extends inner{
      void test(){
        int x = val;
      }
    }
  }
}
