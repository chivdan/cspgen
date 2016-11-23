% MZN file model for IEC 61499 function block ECC induction from scenarios
% Author: Daniil Chivilikhin (chivdan@rain.ifmo.ru)

% given constants and domains

int: C; % machine size
int: V; % scenarios tree size
int: E; % input events count
int: X; % unique input variables sets count
int: O; % output events count
int: L; % output variables count

set of int: colors = 1..C;
set of int: vertices = 1..V;
set of int: input_events = 1..E;
set of int: guards = 1..X;
set of int: output_events = 1..O+1;
set of int: output_variables = 1..L;
set of int: zero_one = 0..1;

%% given data

array [vertices] of 1..E+1: tree_input_event;
array [vertices] of 1..X+1: tree_guards;
array [vertices] of output_events: tree_output_event;
array [vertices] of vertices: tree_parent;
array [vertices, output_variables] of zero_one: tree_z;

%% variables

array [vertices] of var colors: x; % vertices colors
array [colors, input_events, guards] of var colors: y; % transitions destinations
array [colors] of var output_events: o; % output events 
array [colors, output_variables] of var zero_one: d_0; % what to do with zero
array [colors, output_variables] of var zero_one: d_1; % what to do with one 

array [colors, colors] of var bool: tr;
array [2..C] of var colors: p;
array [colors, colors] of var int: m;


%% constraints

% start vertex corresponds to start state
constraint x[1] = 1;

constraint forall (j in output_variables) (
    d_0[1, j] = 0 
);

constraint forall (j in output_variables) (
    d_1[1, j] = 1 
);

% output event constraints
constraint forall (v in 2..V) ( 
    (x[v] != 1) -> (o[x[v]] = tree_output_event[v])
   %(o[x[v]] = tree_output_event[v])
);

constraint o[1] = O+1;

% transitions constraints 
constraint forall (v in 2..V) (
    y[x[tree_parent[v]], tree_input_event[v], tree_guards[v]] = x[v]
);

% redundant constraints to unify y variables
% used to find all solutions
constraint forall (c in colors, e in 1..E, g in 1..X) (
    (not exists (v in 2..V) (tree_input_event[v] == e /\ tree_guards[v] == g /\ x[tree_parent[v]] == c)) -> y[c, e, g] = c
);

% output algorithms constraints
constraint forall (v in 2..V, j in output_variables) (
    (tree_z[tree_parent[v], j] == 0) -> (d_0[x[v], j] = tree_z[v, j])
);

constraint forall (v in 2..V, j in output_variables) (
    (tree_z[tree_parent[v], j] == 1) -> (d_1[x[v], j] = tree_z[v, j])
);

% fix unused variables
constraint forall (j in output_variables, c in colors) (
    (not exists (v in 2..V) (tree_z[tree_parent[v], j] == 1 /\ x[v] == c) -> d_1[c, j] = 1) 
);

constraint forall (j in output_variables, c in colors) ( 
    (not exists (v in 2..V) (tree_z[tree_parent[v], j] == 0 /\ x[v] == c) -> d_0[c, j] = 0)
);

% additional redundant constraints for output variables
constraint forall (v in 2..V, j in output_variables) (
    (tree_z[tree_parent[v], j] != tree_z[v, j]) -> (x[tree_parent[v]] != x[v])
);

% symmetry breaking constraints

% transition definition
constraint forall (i in colors, j in colors) (
    if i < j then
        tr[i, j] = exists (e in 1..E, g in 1..X) (y[i, e, g] = j)
    else 
        tr[i, j] = false
    endif
);

% parent definition
constraint forall (i in colors, j in colors where i < j) (
    (p[j] = i) <-> (tr[i, j] /\ (forall (k in 1..i-1) (not tr[k, j])))
);

% next state i parent is not smaller than our i-1
constraint forall (i in 3..C) (
    p[i - 1] <= p[i]
);

constraint forall (i in colors, j in colors, e in 1..E, g in 1..X) (
    if i < j then 
        (tr[i, j] -> (
    	    (m[i, j] = g) <-> (y[i, e, g] = j /\ (forall (gg in 1..g-1) (y[i, e, gg] != j)))
        ))
        /\
        ((not tr[i, j]) -> m[i, j] = 1)
    else 
       m[i, j] = 0
    endif
);


% ordering nodes i and i+1 with the same parent j
constraint forall (i in colors, j in colors where i < j - 1) (
   (((p[j - 1] = i) /\ (p[j] = i)) -> (m[i, j - 1] < m[i, j]))
);

solve satisfy;                          

output ["# x = ", show(x), "\n",
        "# y = ", show(y), "\n",
        "# o = ", show(o), "\n",
        "# d_0 = ", show(d_0), "\n", 
        "# d_1 = ", show(d_1), "\n",
        "# p = ", show(p), "\n",
        "# tr = ", show(tr), "\n"];
        %"# m = ", show(m), "\n"];
