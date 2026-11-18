package src;

import java.util.List;

public interface Repository<T> {
    void add(T item);
    List<T> getAll();
    void update(T item);
    void delete(int id);
}
