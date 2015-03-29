'use strict';
(function(){

	angular.module('htm.welcome', [])

		.controller('WelcomeCtrl', ['$scope', function($scope) {
		}])

	angular.module('htm.tournament', ['htm.api'])

		.controller('TournamentListCtrl', ['$scope', 'Tournament', function($scope, Tournament) {
			$scope.tournaments = Tournament.query();

			$scope.tournaments.$promise.then(function(tournaments){
				$scope.newTournament.reset();
			},function(error){
				$scope.tournaments.error = error;
			});

			/* New tournament form is initially hidden */
			$scope.addNewTournamentVisible = false;
			$scope.focusOnAddTournament = false;

			$scope.isLoading = function(){
				return !$scope.tournaments.$resolved;
			}
			
			$scope.isError = function(){
				return $scope.tournaments.$resolved && angular.isDefined($scope.tournaments.error);
			}

			$scope.isLoaded = function(){
				return $scope.tournaments.$resolved && angular.isUndefined($scope.tournaments.error);
			}
			
			$scope.showAddNewTournament = function(){
				$scope.addNewTournamentVisible = true;
				$scope.focusOnAddTournament = false;
			}

			$scope.hideAddNewTournament = function(){
				$scope.addNewTournamentVisible = false;
				$scope.focusOnAddTournament = true;
			}

			$scope._findTournamentWithSameName = function(newTournament){
				return _.find($scope.tournaments, function(existingTournament){ 
					return existingTournament.name === newTournament.name; 
				});
			};

			$scope.newTournament = {
				_defaultName : 'Basic Longsword Tournament',
				name: '',
				memo: undefined,

				customMemo: false,
				error: undefined,

				updateMemo: function(memo){
					if(angular.isDefined(memo)){
						this.memo = memo;
						this.customMemo = true;
					};

					// Reset memo when not custom or undefined
					if(!this.customMemo && angular.isUndefined(memo)){
						this.memo = this._defaultMemo();
					}
					return this.memo;
				},

				_defaultMemo: function() {
					var memo = '';
					var name = this.name || '';

					angular.forEach(name.toUpperCase().split(' '), function(s) {
						if(isNaN(s)){
							memo += s.charAt(0);
						} else {
							memo += s;
						}
					});
					return memo;
				},

				_generateUniqueName: function(){
					var i = 2;
					while($scope._findTournamentWithSameName(this)){
						this.name =  this._defaultName + " " + i++;
					}
				},

				reset: function() {
					this.name = this._defaultName;
					this.customIdentifier = false;
					this._identifier = undefined;
					this.customMemo = false;
					this.memo = undefined;
					this.error = undefined;
					this._generateUniqueName();
					this.updateMemo();
				}
			};
			$scope.newTournament.reset();

			$scope.save = function() {
				var t = $scope.newTournament;
				var tournament = new Tournament({
					name: t.name,
					memo: t.updateMemo(),
					participants: []
				});

				tournament.$save().then(function(savedTournament){
					$scope.tournaments.push(savedTournament);
					$scope.newTournament.reset();
					$scope.hideAddNewTournament();
				},function(error){
					$scope.newTournament.error = error;
				});					
			};
		}])

		.controller('TournamentCtrl', ['$scope', '$routeParams', 'Tournament', function($scope, $routeParams, Tournament) {

		}])

	angular.module('htm.particpant', [])

		.controller('ParticipantListCtrl', ['$scope', '$modal','$routeParams','Tournament','Participant', function($scope,$modal,$routeParams,Tournament, Participant) {
			  

			  //TODO: Fetch totals
			
			 //TODO: Get totals
			$scope.totals = {
				participants: 100,
				clubs: 1,
				countries: 10
			}

			$scope.participants = Participant.query();
			$scope.tournaments = Tournament.query();



			$scope.refresh = function(){
					console.log("Refresh");
			};
			$scope.registerSelected = function(){
					console.log("registerSelectedParticipants");
			};
			$scope.unregisterSelected = function(){
					console.log("unregisterSelected");
			};
		  	$scope.add = function(){
		  		openModal({ 
						country: {},
						isPresent: false,
						previousWins: [ ],
						subscriptions: [],
						hasPicture: false
					});
			};

		  	$scope.hasDetails = function(participant) {
		    	return participant.age || participant.height || participant.weight;
  			};

  			$scope.show = function(participant){
  				openModal(participant);
  			}

			function openModal(participant) {

				$modal.open({
				  templateUrl: '/partials/participant-registration.html',
				  controller: 'ParticipantRegistrationCtrl',
				  size: 'lg',
				  resolve: {
				    participant: function () {
				      return participant;
				    },
				    tournaments: function() {
				      return $scope.tournaments;
				    }
				  }
				})
				.result.then(function(updatedParticipant) {

					updatedParticipant = new Participant(updatedParticipant);

					updatedParticipant.$save().then(function(savedParticipant){
						//TODO: Check if already present
						$scope.participants.push(savedParticipant);
					}, function(error){
						//TODO: Handle errors
					});
				  
				});
			};

			if($routeParams.participantId){
				var participant = Participant.get({id:$routeParams.participantId});
				openModal(participant)
			}

		}])

		.controller('ParticipantRegistrationCtrl',function($scope, $modalInstance, Country, Participant, participant, tournaments) {

			$scope.pictures = {files:[]};
			$scope.participant = angular.copy(participant);
			$scope.tournaments = _.filter(tournaments,function(tournament){
				return !_.find(participant.subscriptions,function(subscription){
					return subscription.tournament.id === tournament.id;
				})
			});
			$scope.countries = Country.query();

			$scope.addWin = function() {
				if ($scope.canAddWin()) {
				  $scope.participant.previousWins.push({text: ''});
				}
			};

			$scope.canAddWin = function() {
				return $scope.participant.previousWins.length < 3;
			};

			$scope.removeWin = function(win) {
				$scope.participant.previousWins = _($scope.participant.previousWins).filter(function(item) {
   					  return item !== win
				});
			};

			$scope.save = function() {
				$modalInstance.close($scope.participant);
			};

			$scope.checkin = function() {
				$scope.participant.isPresent = true;
				$scope.save();
			};

			$scope.cancel = function() {
				$modalInstance.dismiss('cancel');
			};

			$scope.upload = function(pictures){
				var picture = new Participant({id:$scope.participant.id,file:pictures[0]});
				picture.$postPicture().then(function(participant){
					// TODO: Update participant id with new id assigned by server 
					// TODO: Set hasPicture = true;
				},function(error){
					// TODO: Handle error
				});
			};


		})
	;

})();