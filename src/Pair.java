public class Pair<X, Y>{
    private volatile  X first;
    private volatile  Y second;

    public Pair(X first, Y second){
        this.first = first;
        this.second = second;
    }

    public X first(){
        return this.first;
    }

    public Y second(){
        return this.second;
    }

    public void setState(Y newState){
        this.second = newState;
    }

    public Y getState(){
        return this.second;
    }
}
