$ ->
  if (window.autoRefresh)
    window.autoRefresh.postRefresh ->
      $('#chargeModal').modal()
  else
    $('#chargeModal').modal()
