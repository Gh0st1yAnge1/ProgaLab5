package org.example.command;

import org.example.manager.CollectionManager;
import org.example.model.Route;
import org.example.request_and_response.Response;

public class AverageOfDistance implements Command{

    private final CollectionManager collectionManager;

    public AverageOfDistance(CollectionManager collectionManager){
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(String arg, Route route) {

        if (arg != null){
            return new Response(false, "Usage: average_of_distance", null);
        }

        double avgDist = collectionManager.averageOfDistance();

        return new Response(true, "Average distance: " + avgDist, null);
    }

    public String getName() {
        return "average_of_distance";
    }

    @Override
    public String getDescription() {
        return "gives you average of distance fields in collection";
    }
}
