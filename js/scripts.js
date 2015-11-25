function executeComposable (composable, data, input) {
    var manager = new sweva.ExecutionManager();
    manager.setup(composable);
    
    manager.execute(
    JSON.parse(data),
    JSON.parse(input)
    ).then (function(result){
        makeResult({result:result});
    });
}