$ ->
  $('#setCookie').click (e) ->
    e.preventDefault()
    jsRoutes.controllers.Login.seenGc2Modal().ajax
      context: this
      success: ->
        $('#gc2Modal').modal('hide')
  $('#gc2Modal').modal()

