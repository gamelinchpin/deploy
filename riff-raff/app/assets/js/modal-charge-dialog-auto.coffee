seenDialog = false

$ ->
  if (window.autoRefresh)
    window.autoRefresh.postRefresh ->
      if !seenDialog
        $('#chargeModal').modal()
        seenDialog = true
  else
    if !seenDialog
      $('#chargeModal').modal()
      seenDialog = true
