angular.module("common.filters", []).
  filter("property", function() { return function(value, property) {
    if (angular.isObject(value)) {
      if (value.hasOwnProperty(property)) {
        return value[property];
      }
    }
  }}).
  
  filter("size", function() { return function(value) {
	 return value.length; 
  }}).
  
  filter("inRangeOf", function() { return function(value, index) {
	  if (angular.isArray(value)) {
		  return value.slice(index - 2, index + 2);
	  }
  }});
  
