# E-Commerce Database Access

To do integration testing of services, use sample code in `ECommerceTestApp`.

## Customer Service

One by one, implement methods of `CustomerService`.
For this, repeatedly perform the following steps.

1. Implement required method in `CustomerDBIO` class
2. Implement required method in `DbCustomerMapper` class or `CustomerMapper` class
3. Implement targeted `OrderService` method
4. Test this last method in integration

## Order Service

Implement method of `OrderService`

1. Implement `findDb` method of `OrderDB`
2. Implement `map` method  of `OrderMapper`
3. Implement `find` method of `OrderDB`
4. Implement `find` method of `OrderService`
5. Test this last method in integration
