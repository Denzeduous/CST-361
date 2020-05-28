package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.UserModel;

/**
 * Servlet implementation class SearchController
 */
@WebServlet("/UserController")
public class UserController extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public UserController() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// Note: Don't actually do this. Double bracket initializers
		// have their problems. I just did it because I'm lazy.
		//
		// Didn't create an action, since it would be highly unnecessary
		// in this very simplistic example. Just pretend this section
		// is in an action if you want to.
		List<UserModel> users = new ArrayList<UserModel>() {{
			add(new UserModel("Bob",     34));
			add(new UserModel("Joe",     26));
			add(new UserModel("Richard", 54));
		}};
		
		request.setAttribute("users", users);
		
		RequestDispatcher rd = request.getRequestDispatcher("/users.jsp");
		rd.forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
