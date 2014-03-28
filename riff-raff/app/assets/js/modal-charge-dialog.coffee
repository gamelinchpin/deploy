clickFromModalDialog = false

displayDialog = ->
  $('#chargeModal').modal()

  $('#chargeUpdateOK').click =>
    jsRoutes.controllers.Application.seenChargeDialog().ajax
      context: this
      success: ->
        $('#chargeModal').modal('hide')

$ ->
  if (window.autoRefresh)
    window.autoRefresh.postRefresh ->
      displayDialog()
  else
    displayDialog()
