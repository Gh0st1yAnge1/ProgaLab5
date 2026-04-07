package org.example.manager;

import org.example.model.Route;
import org.example.utils.IdGenerator;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


public class CollectionManager {
    private LinkedHashMap<Integer, Route> collection;
    private LocalDate initializationDate;

    public CollectionManager(){
        this.collection = new LinkedHashMap<>();
        this.initializationDate = LocalDate.now();
    }

    public boolean checkKey(Integer id){
        return collection.containsKey(id);
    }

    public boolean insert(Integer id, Route route){
        if (collection.putIfAbsent(id, route) == null){
            IdGenerator.compareMaxId(route.getId());
            return true;
        }
        return false;
    }


    public boolean update(Integer id, Route route){
        if (collection.containsKey(id)){
            collection.replace(id, route);
            return true;
        }
        return false;
    }


    public boolean removeByKey(Integer id){
        int sizeBefore = collection.size();
        collection.entrySet().removeIf(entry -> entry.getKey().equals(id));
        return collection.size() < sizeBefore;
    }

    public void clear(){
        collection.clear();
    }

    public Map<Integer, Route> show(){
        return Collections.unmodifiableMap(collection);
    }

    public int removeGreater(Route route){
        int sizeBefore = collection.size();
//        Iterator<Map.Entry<Integer, Route>> iterator = collection.entrySet().iterator();
//
//        while(iterator.hasNext()){
//            Map.Entry<Integer, Route> entry = iterator.next();                               вайб темка на понимание
//
//            if (entry.getValue().compareTo(route) > 0){
//                iterator.remove();
//            }
//        }
        collection.entrySet().removeIf(entry -> entry.getValue().compareTo(route) > 0);
        return sizeBefore - collection.size();
    }

    public boolean replaceIfLower(Integer id, Route route){
        if (collection.containsKey(id)){
            if (collection.get(id).compareTo(route) > 0){
                collection.replace(id, route);
                return true;
            }
            return false;
        }
        return false;
    }

    public int removeGreaterKey(Integer id){
        int sizeBefore = collection.size();
        collection.entrySet().removeIf(entry -> entry.getKey().compareTo(id) > 0);
        return sizeBefore - collection.size();
    }

    public double averageOfDistance(){
        return collection.values().stream().mapToDouble(Route::getDistance).average().orElse(0.0);
    }

    public long countByDistance(double distance){
        return (collection.values().stream().filter(route -> route.getDistance() == distance).count());
    }

    public List<Route> filterLessThanDistance(double distance){
        return collection.values().stream().filter(route -> route.getDistance() < distance).collect(Collectors.toList());
    }

    public void info(){
        System.out.println("Collection type: LinkedHashMap");
        System.out.println("Size: " + collection.size());
        System.out.println("Initialization date: " + initializationDate);
    }
}