angular.module('lwResourcesDirective', [])

.directive('resourceslist', function () {
    return {
        restrict: "E",
        replace: true,
        scope: {
            list: '=',
            parent: '='
        },
        template: "<ul'><resource ng-repeat='resource in list' resource='resource' parent='parent'></resource></ul>"
    }
})

.directive('resource', function ($compile, $routeParams, $http, dialog,$filter) {
    return {
        restrict: "E",
        replace: true,
        scope: {
            resource: '=',
            parent: '='
        },
        templateUrl: "partials/resource.html",
        link: function (scope, element, attrs) {
            // compute path and tree depth
            var parentPath = "";
            var treeDepth = 0;
            if(scope.parent) {
                parentPath = scope.parent.path;
                treeDepth = scope.parent.treeDepth + 1;
            }
            scope.resource.path = parentPath + "/" + scope.resource.id;
            scope.resource.treeDepth = treeDepth;
            scope.resource.read  =  {tooltip : "Read <br/>"   + scope.resource.path};
            scope.resource.write =  {tooltip : "Write <br/>"  + scope.resource.path};
            scope.resource.exec  =  {tooltip : "Execute <br/>"+ scope.resource.path};
            scope.resource.observe  =  {tooltip : "Observe <br/>"+ scope.resource.path};
            
            scope.arraypath = function (){
                var path = scope.resource.path;
                if (typeof path === "string"){
                    var res = path.split("/");
                    res.shift();
                    return res;
                }
                return [];
            }

            scope.observable = function() {
                if(scope.resource.instances != "multiple") {
                    if(scope.resource.hasOwnProperty("operations")) {
                        return scope.resource.operations.indexOf("R") != -1;
                    }
                }
                return false;
            }
                        
            scope.readable = function() {
                if (scope.arraypath().length == 2){
                    // read allow for object instance
                    return true;
                }else if(scope.observable()){
                    return true;
                }
                return false;
            }

            scope.writable = function() {
                if(scope.resource.instances != "multiple") {
                    if(scope.resource.hasOwnProperty("operations")) {
                        return scope.resource.operations.indexOf("W") != -1;
                    }
                }
                return false;
            }

            scope.executable = function() {
                if(scope.resource.instances != "multiple") {
                    if(scope.resource.hasOwnProperty("operations")) {
                        return scope.resource.operations.indexOf("E") != -1;
                    }
                }
                return false;
            }

            scope.startObserve = function() {
                var uri = "api/clients/" + $routeParams.clientId + scope.resource.path+"/observe";
                $http.post(uri)
                .success(function(data, status, headers, config) {
                    var observe = scope.resource.observe;
                    observe.date = new Date();
                    var formattedDate = $filter('date')(observe.date, 'HH:mm:ss.sss');
                    observe.status = data.status;
                    observe.tooltip = formattedDate + "<br/>" + observe.status;
                    
                    // HACK
                    if (data.status == "CONTENT" || data.status == "CHANGED") {
                        scope.resource.observed = true;
                        scope.resource.value = data.value;
                        scope.resource.valuesupposed = false;
                        scope.resource.tooltip = formattedDate;
                    }
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to start observation on resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };
            
            scope.stopObserve = function() {
                var uri = "api/clients/" + $routeParams.clientId + scope.resource.path + "/observe";
                $http.delete(uri)
                .success(function(data, status, headers, config) {
                    scope.resource.observed = false;
                    scope.resource.observe.stop = "success";
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to stop observation on resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };
            
            
            scope.read = function() {
                var uri = "api/clients/" + $routeParams.clientId + scope.resource.path;                
                $http.get(uri)
                .success(function(data, status, headers, config) {
                    // manage request information
                    var read = scope.resource.read;
                    read.date = new Date();
                    var formattedDate = $filter('date')(read.date, 'HH:mm:ss.sss');
                    read.status = data.status;
                    read.tooltip = formattedDate + "<br/>" + read.status;
                    
                    // manage read data
                    if (data.status == "CONTENT") {
                        if (scope.arraypath().length == 2){
                            for (var i in data.value) {
                                var res = data.value[i];
                                scope.resource.values[res.id].value = res.value;
                                scope.resource.values[res.id].valuesupposed = false;
                                scope.resource.values[res.id].tooltip = formattedDate;
                            }
                        }else{
                            scope.resource.value = data.value;
                            scope.resource.valuesupposed = false;
                            scope.resource.tooltip = formattedDate;
                        }
                    }
                }).error(function(data, status, headers, config) {
                    if (observe) {
                        scope.resource.observe.status = false;
                    }
                    errormessage = "Unable to read resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };

            scope.write = function() {
                $('#writeModalLabel').text(scope.resource.name);
                $('#writeInputValue').val(scope.resource.value);
                $('#writeSubmit').unbind();
                $('#writeSubmit').click(function(e){
                    e.preventDefault();
                    var value = $('#writeInputValue').val();
                    if(value) {
                        $('#writeModal').modal('hide');
                        $http({method: 'PUT', url: "api/clients/" + $routeParams.clientId + scope.resource.path, data: value, headers:{'Content-Type': 'text/plain'}})
                        .success(function(data, status, headers, config) {
                            write = scope.resource.write;
                            write.date = new Date();
                            var formattedDate = $filter('date')(write.date, 'HH:mm:ss.sss');
                            write.status = data.status;
                            write.tooltip = formattedDate + "<br/>" + write.status;
                            
                            if (data.status == "CHANGED") {
                                scope.resource.value = value;
                                scope.resource.valuesupposed = true;
                                scope.resource.tooltip = formattedDate;
                            }
                        }).error(function(data, status, headers, config) {
                            errormessage = "Unable to write resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                            dialog.open(errormessage);
                            console.error(errormessage)
                        });;
                    }
                });

                $('#writeModal').modal('show');
            };

            scope.exec = function() {
                $http.post("api/clients/" + $routeParams.clientId+ scope.resource.path)
                .success(function(data, status, headers, config) {
                    var exec = scope.resource.exec;
                    exec.date = new Date();
                    var formattedDate = $filter('date')(exec.date, 'HH:mm:ss.sss');
                    exec.status = data.status;
                    exec.tooltip = formattedDate + "<br/>" + exec.status;
                }).error(function(data, status, headers, config) {
                    errormessage = "Unable to execute resource " + scope.resource.path + " for "+ $routeParams.clientId + " : " + status +" "+ data
                    dialog.open(errormessage);
                    console.error(errormessage)
                });;
            };

            // add children
            var collectionSt = '<resourceslist list="resource.values" parent="resource"></resourceslist>';
            if (angular.isArray(scope.resource.values)) {
                $compile(collectionSt)(scope, function(cloned, scope)   {
                    element.append(cloned);
                });
            }
        }
    }
});
