package org.example.command;

import org.example.manager.CollectionManager;
import org.example.model.Route;
import org.example.request_and_response.Response;
import java.util.List;

public class FilterLessThanDistance implements Command {

    private final CollectionManager collectionManager;

    public FilterLessThanDistance(CollectionManager collectionManager){
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(String arg, Route route) {

        if (arg == null || arg.isBlank()){
            return new Response(false, "Usage: filter_less_than_distance <distance>", null);
        }

        List<Route> out;
        try{
            double distance = Double.parseDouble(arg);
            out = collectionManager.filterLessThanDistance(distance);
        } catch (NumberFormatException ex){
            return new Response(false, "Usage: filter_less_than_distance <distance>", null);
        }

        return new Response(true, "There are routes, you have got:\n", out);

    }

    @Override
    public String getName() {
        return "filter_less_than_distance";
    }

    @Override
    public String getDescription() {
        return "shows elements, which distance field\nis less than inserted value";
    }
}
