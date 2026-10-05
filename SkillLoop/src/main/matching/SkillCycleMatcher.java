package skillloop.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import skillloop.collections.Repository;
import skillloop.skills.Skill;
import skillloop.users.Student;

/**
 * ============================================================================
 * THE INNOVATIVE FEATURE OF SKILLLOOP: SKILL CYCLE MATCHING
 * ============================================================================
 *
 * The problem:
 *   A wants Python but cannot teach Python-people anything they want.
 *   A normal "who teaches Python?" search finds nothing useful for everybody.
 *
 * The idea:
 *   Treat the students as a GRAPH.
 *   Draw an arrow  A --Java--> B  whenever  A can teach Java AND B wants Java.
 *   A closed loop in that graph is a group where every student teaches one
 *   person and learns from another, so everybody gains something:
 *
 *        A --Java--> B --Python--> C --C--> A
 *
 * The algorithm (plain DFS - depth first search):
 *   1. Build the graph: for every ordered pair of students, add an arrow for
 *      each skill one can teach and the other wants.
 *   2. From every student, walk the arrows step by step, remembering the path.
 *   3. If a step leads back to the student we started from, we found a cycle.
 *   4. Store each cycle only once (the same loop can be found starting from any
 *      of its members), by always writing it down starting from the smallest id.
 *
 * It is ordinary graph traversal - no AI, nothing hidden, and easy to draw on
 * the board during the viva.
 */
public class SkillCycleMatcher {

    /** Cycles longer than this are not useful in practice (and slow to search). */
    public static final int DEFAULT_MAX_CYCLE_LENGTH = 4;

    private final Repository<Student> studentRepository;
    private final int maxCycleLength;

    /** DEPENDENCY INJECTION again: the repository is handed in, not created here. */
    public SkillCycleMatcher(Repository<Student> studentRepository) {
        this(studentRepository, DEFAULT_MAX_CYCLE_LENGTH);
    }

    public SkillCycleMatcher(Repository<Student> studentRepository, int maxCycleLength) {
        this.studentRepository = studentRepository;
        this.maxCycleLength = maxCycleLength;
    }

    /** One arrow of the graph: "teacher can teach 'skill' to learner". */
    private static class Edge {
        final Student to;
        final String skill;

        Edge(Student to, String skill) {
            this.to = to;
            this.skill = skill;
        }
    }

    /**
     * STEP 1 - build the adjacency map (the graph).
     * MAP: student id -> list of arrows leaving that student.
     */
    private Map<Integer, List<Edge>> buildGraph() {
        Map<Integer, List<Edge>> graph = new HashMap<>();
        List<Student> students = studentRepository.getAll();

        for (Student teacher : students) {
            List<Edge> edges = new ArrayList<>();
            for (Student learner : students) {
                if (teacher.getId() == learner.getId()) {
                    continue;
                }
                for (Skill skill : teacher.getTeachingSkills()) {
                    if (learner.wantsToLearn(skill.getName())) {
                        edges.add(new Edge(learner, skill.getName()));
                    }
                }
            }
            graph.put(teacher.getId(), edges);
        }
        return graph;
    }

    /**
     * STEP 2..4 - find every cycle in the graph.
     *
     * @return cycles of length 2 (direct exchange) up to maxCycleLength
     */
    public List<SkillCycle> findAllCycles() {
        Map<Integer, List<Edge>> graph = buildGraph();
        List<SkillCycle> cycles = new ArrayList<>();
        Set<String> alreadyFound = new HashSet<>();     // SET prevents duplicate cycles

        for (Student start : studentRepository.getAll()) {
            List<Student> path = new ArrayList<>();
            List<String> skillsOnPath = new ArrayList<>();
            path.add(start);
            explore(graph, start, start, path, skillsOnPath, cycles, alreadyFound);
        }
        return cycles;
    }

    /**
     * Recursive depth-first walk.
     *
     * @param current      where we are standing now
     * @param start        where the walk began (a cycle must return here)
     * @param path         the students visited so far, in order
     * @param skillsOnPath the skill taught on each step taken so far
     */
    private void explore(Map<Integer, List<Edge>> graph,
                         Student current,
                         Student start,
                         List<Student> path,
                         List<String> skillsOnPath,
                         List<SkillCycle> cycles,
                         Set<String> alreadyFound) {

        if (path.size() > maxCycleLength) {
            return;                                   // walked too far, give up
        }

        for (Edge edge : graph.getOrDefault(current.getId(), new ArrayList<>())) {

            if (edge.to.getId() == start.getId() && path.size() >= 2) {
                // the arrow leads back to the start -> we closed a cycle
                List<String> cycleSkills = new ArrayList<>(skillsOnPath);
                cycleSkills.add(edge.skill);
                String key = cycleKey(path);
                if (alreadyFound.add(key)) {          // add() is false if already present
                    cycles.add(new SkillCycle(new ArrayList<>(path), cycleSkills));
                }
                continue;
            }

            if (containsStudent(path, edge.to)) {
                continue;                             // never visit the same student twice
            }

            // go one step deeper
            path.add(edge.to);
            skillsOnPath.add(edge.skill);

            explore(graph, edge.to, start, path, skillsOnPath, cycles, alreadyFound);

            // BACKTRACK: undo the step so other branches can be tried
            path.remove(path.size() - 1);
            skillsOnPath.remove(skillsOnPath.size() - 1);
        }
    }

    private boolean containsStudent(List<Student> path, Student student) {
        for (Student visited : path) {
            if (visited.getId() == student.getId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The same loop A->B->C->A can also be discovered as B->C->A->B.
     * To store it only once we rotate the ids so that the smallest id comes
     * first, and use the resulting text as the key of a Set.
     */
    private String cycleKey(List<Student> path) {
        int smallestIndex = 0;
        for (int i = 1; i < path.size(); i++) {
            if (path.get(i).getId() < path.get(smallestIndex).getId()) {
                smallestIndex = i;
            }
        }
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            int index = (smallestIndex + i) % path.size();
            key.append(path.get(index).getId()).append("-");
        }
        return key.toString();
    }

    /** Only the cycles that a particular student takes part in. */
    public List<SkillCycle> findCyclesFor(Student student) {
        List<SkillCycle> mine = new ArrayList<>();
        for (SkillCycle cycle : findAllCycles()) {
            if (containsStudent(cycle.getMembers(), student)) {
                mine.add(cycle);
            }
        }
        return mine;
    }

    /** Cycles with 3 or more members (the headline feature of the project). */
    public List<SkillCycle> findMultiPersonCycles() {
        List<SkillCycle> multi = new ArrayList<>();
        for (SkillCycle cycle : findAllCycles()) {
            if (!cycle.isDirectExchange()) {
                multi.add(cycle);
            }
        }
        return multi;
    }
}
