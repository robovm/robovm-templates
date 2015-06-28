package ${package};

public class CounterStore {
    private int count;

    public void Add(int num) {
        count += num;
    }

    public int Get() {
        return count;
    }
}
