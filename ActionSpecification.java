/**
 * An example class for demonstrating usage of subqueries in a Criteria Query within Spring Specifications
 * for creating complex queries
 */

@Service
public class ActionSpecification {

    private final SupportService supportService;

    public ActionSpecification(SuppurtService supportService) {
        this.supportService = supportService;
    }

    // select ONLY distinct actions where scope types != 7 and scope values in routes and in visit coordinates
    public Specification<Action> byVisitCoordinatesAndRoutes() {
        return ((root, criteriaQuery, criteriaBuilder) -> {

            Set<Route> routes = supportService.getRoutes();
            if (routes == null || routes.isEmpty())
                return criteriaBuilder.disjunction();

            Set<VisitCoordinates> visitCoordinates = supportService.getVisitCoordinates();
            if (visitCoordinates == null || visitCoordinates.isEmpty())
                return criteriaBuilder.disjunction();

            Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
            Root subRoot = subquery.from(Action.class);
            Join<Action,Scope> subScopes = subRoot.join("scopes");
            subquery.select(subRoot.get("id")).distinct(true).where(criteriaBuilder.equal(subScopes.get("scopeTypeId"), 7L));

            Join<Scope, Action> joinScope = root.join("scopes");
            Set<Long> routeIds = routes.stream().map(Route::getId).collect(Collectors.toSet());
            Set<Long> visitCoordinatesIds = visitCoordinates.stream().map(VisitCoordinate::getId).collect(Collectors.toSet());
            return criteriaBuilder.or(
                criteriaBuilder.and(
                    criteriaBuilder.equal(joinScope.get("scopeTypeId"), 5L),
                    joinScope.get("value").in(routeIds),
                    root.get("id").in(subquery).not()),
                criteriaBuilder.and(
                    criteriaBuilder.equal(joinScope.get("scopeTypeId"), 6L),
                    joinScope.get("value").in(visitCoordinatesIds),
                    root.get("id").in(subquery).not()
            ));
        });
    }

    // select actions only for routes (with scopes "ScopeTypeId" not in (7,6), but can be = 0)
    public Specification<Action> byRoutes() {
        return ((root, criteriaQuery, criteriaBuilder) -> {

            Set<Route> routes = supportService.getRoutes();
            if (routes == null || routes.isEmpty())
                return criteriaBuilder.disjunction();

            // subquery is needed to exclude results where "ScopeTypeId" = 7 and 6
            Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
            Root subRoot = subquery.from(Action.class);
            Join<Action,Scope> subScopes = subRoot.join("scopes");
            subquery.select(subRoot.get("id")).distinct(true);
            subquery.where(criteriaBuilder.or(
                criteriaBuilder.equal(subScopes.get("scopeTypeId"), 7L),
                criteriaBuilder.and(
                    criteriaBuilder.equal(subScopes.get("scopeTypeId"), 6L),
                    criteriaBuilder.equal(subScopes.get("value"), 0).not()
                ))
            );

            Join<Action, Scope> joinScope = root.join("scopes");
            Set<Long> routeIds = routes.stream().map(Route::getId).collect(Collectors.toSet());
            return criteriaBuilder.and(
                criteriaBuilder.equal(joinScope.get("scopeTypeId"), 5L),
                joinScope.get("value").in(routeIds),
                root.get("id").in(subquery).not()
            );
        });
    }

    // select actions set only for visit coordinates (with scopes "scopeTypeId" not in (7,5))
    public Specification<Action> byVisitCoordinates() {
        return (root, criteriaQuery, criteriaBuilder) -> {

            Set<VisitCoordinate> visitCoordinates = supportService.getVisitCoordinates();
            if (visitCoordinates == null || visitCoordinates.isEmpty())
                return criteriaBuilder.disjunction();

            Join<Action, Scope> joinScope = root.join("scopes");

            // subquery is needed to exclude results where "scopeTypeId" = 7 and 5
            Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
            Root subRoot = subquery.from(Action.class);
            Join<Action,Scope> subScopes = subRoot.join("scopes");
            subquery.select(subRoot.get("id")).distinct(true);
            subquery.where(criteriaBuilder.or(
                criteriaBuilder.equal(subScopes.get("scopeTypeId"), 7L),
                criteriaBuilder.equal(subScopes.get("scopeTypeId"), 5L))
            );

            Set<Long> visitCoordinatesIds = visitCoordinates.stream().map(VisitCoordinate::getId).collect(Collectors.toSet());

            return criteriaBuilder.and(
                criteriaBuilder.equal(joinScope.get("scopeTypeId"), 6L),
                joinScope.get("value").in(visitCoordinatesIds),
                root.get("id").in(subquery).not()
            );
        };
    }

    // Select actions set to customer (scope type = 7)
    public Specification<Action> byCustomer() {
        return (root, criteriaQuery, criteriaBuilder) ->  {

            Customer customer = supportService.getCustomer();
            if (customer == null)
                return criteriaBuilder.disjunction();

            Join<Action,Scope> joinScope = root.join("scopes");
            Predicate predicate = builder.and(
                criteriaBuilder.equal(joinScope.get("scopeTypeId"), 7L),
                criteriaBuilder.equal(joinScope.get("value"), customer.getId()));

            return criteriaBuilder.and(predicate);

        };
    }
}
