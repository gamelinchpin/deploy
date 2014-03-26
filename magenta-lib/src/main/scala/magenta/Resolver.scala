package magenta

import tasks.Task

case class RecipeTasks(recipe: Recipe, preTasks: List[Task], hostTasks: List[Task], disabled: Boolean = false) {
  lazy val hosts = tasks.flatMap(_.taskHost).map(_.copy(connectAs=None)).distinct
  lazy val tasks = if (disabled) Nil else preTasks ++ hostTasks
  lazy val recipeName = recipe.name
}

case class RecipeTasksNode(recipeTasks: RecipeTasks, children: List[RecipeTasksNode]) {
  def disable(f: RecipeTasks => Boolean): RecipeTasksNode = {
    if (f(recipeTasks))
      copy(recipeTasks = recipeTasks.copy(disabled=true), children = children.map(_.disable(_ => true)))
    else
      copy(children = children.map(_.disable(f)))
  }

  def toList: List[RecipeTasks] = children.flatMap(_.toList) ++ List(recipeTasks)
}

object Resolver {

  def resolve( project: Project, resourceLookup: Lookup, parameters: DeployParameters): List[Task] =
    resolveDetail(project, resourceLookup, parameters).flatMap(_.tasks)

  def resolveDetail( project: Project, resourceLookup: Lookup, parameters: DeployParameters): List[RecipeTasks] = {

    def resolveTree(recipeName: String, stack: Stack): RecipeTasksNode = {
      val recipe = project.recipes.getOrElse(recipeName, sys.error(s"Recipe '$recipeName' doesn't exist in your deploy.json file"))
      val recipeTasks = resolveRecipe(recipe, stack)
      val children = recipe.dependsOn.map(resolveTree(_, stack))
      RecipeTasksNode(recipeTasks, children)
    }

    def resolveRecipe(recipe: Recipe, stack: Stack): RecipeTasks = {
      val tasksToRunBeforeApp = recipe.actionsBeforeApp.toList flatMap { _.resolve(resourceLookup, parameters, stack) }

      val perHostTasks = {
        for {
          action <- recipe.actionsPerHost
          tasks <- action.resolve(resourceLookup, parameters, stack)
        } yield {
          tasks
        }
      }

      val taskHosts = perHostTasks.flatMap(_.taskHost).toSet
      val taskHostsInOriginalOrder = resourceLookup.instances.all.filter(h => taskHosts.contains(h.copy(connectAs = None)))
      val groupedHosts = taskHostsInOriginalOrder.transposeBy(_.tags.getOrElse("group",""))
      val sortedPerHostTasks = perHostTasks.toList.sortBy(t =>
        t.taskHost.map(h => groupedHosts.indexOf(h.copy(connectAs = None))).getOrElse(-1)
      )

      RecipeTasks(recipe, tasksToRunBeforeApp, sortedPerHostTasks)
    }

    val stacks = parameters.stacks match {
      case Nil => if (project.defaultStacks.nonEmpty) project.defaultStacks else Seq(UnnamedStack)
      case s => s
    }

    for {
      stack <- stacks.toList
      tasks <- {
        val resolvedTree = resolveTree(parameters.recipe.name, stack)
        val filteredTree = resolvedTree.disable(rt => !rt.recipe.actionsPerHost.isEmpty && rt.hostTasks.isEmpty)
        filteredTree.toList.distinct
      }
    } yield tasks
  }

}
class NoHostsFoundException extends Exception("No hosts found")




